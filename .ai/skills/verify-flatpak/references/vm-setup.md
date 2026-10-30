# Building the `feedflow-flatpak` VM

A local arm64 Ubuntu VM in UTM on an Apple Silicon Mac. Everything is scripted; no installer
clicking. Tested with macOS 26, UTM 5.0.8 and Ubuntu 26.04. `$VMDIR` is any folder outside the
repo (for example `~/VMs/feedflow-flatpak`).

## 1. Host tools

```bash
brew install --cask utm   # also links utmctl
brew install qemu         # only for qemu-img (UTM's bundled one is a framework, not a CLI)
```

No Rosetta: the VM builds aarch64 only and x86_64 is checked statically.

## 2. Disk image

```bash
mkdir -p "$VMDIR" && cd "$VMDIR"
curl -fLO https://cloud-images.ubuntu.com/releases/26.04/release/ubuntu-26.04-server-cloudimg-arm64.img
curl -sL https://cloud-images.ubuntu.com/releases/26.04/release/SHA256SUMS \
  | grep ' \*ubuntu-26.04-server-cloudimg-arm64.img$' | sed 's/ \*/  /' | shasum -a 256 -c
qemu-img convert -O raw ubuntu-26.04-server-cloudimg-arm64.img disk.img   # Apple backend needs raw
qemu-img resize -f raw disk.img 120G                                      # sparse
```

## 3. cloud-init seed (NoCloud)

`seed/user-data` (use the same user name as on the Mac, so the default SSH target works):

```yaml
#cloud-config
hostname: feedflow-flatpak
users:
  - name: <your user name>
    shell: /bin/bash
    groups: [sudo, adm]
    sudo: ALL=(ALL) NOPASSWD:ALL
    ssh_authorized_keys:
      - <your SSH public key>
ssh_pwauth: false
package_update: true
packages: [openssh-server, avahi-daemon]
```

`seed/meta-data`: `instance-id: feedflow-flatpak-1` and `local-hostname: feedflow-flatpak`.

```bash
hdiutil makehybrid -o seed.iso seed -iso -joliet -default-volume-name cidata
```

`avahi-daemon` is what makes `feedflow-flatpak.local` resolve from the Mac.

## 4. Create the VM

```bash
osascript - "$VMDIR" <<'EOF'
on run argv
  set vmDir to item 1 of argv
  tell application "UTM"
    make new virtual machine with properties {backend:apple, configuration:{name:"feedflow-flatpak", ¬
      cpu cores:6, memory:12288, drives:{{removable:true, source:POSIX file (vmDir & "/seed.iso")}, ¬
      {source:POSIX file (vmDir & "/disk.img")}}}}
  end tell
end run
EOF
```

Copying the 120 GB sparse disk makes the AppleScript call time out (`-1712`), but the VM is still
created. Then quit UTM and patch the bundle's `config.plist`, because AppleScript leaves no display
and disables keyboard and pointer:

```bash
B=~/Library/Containers/com.utmapp.UTM/Data/Documents/feedflow-flatpak.utm
cp "$VMDIR/seed.iso" "$B/Data/seed.iso"
/usr/libexec/PlistBuddy \
  -c 'Add :Display:0 dict' -c 'Add :Display:0:WidthPixels integer 1920' \
  -c 'Add :Display:0:HeightPixels integer 1200' -c 'Add :Display:0:PixelsPerInch integer 80' \
  -c 'Add :Display:0:DynamicResolution bool true' \
  -c 'Set :Virtualization:Keyboard Generic' -c 'Set :Virtualization:Pointer Mouse' \
  -c 'Add :Drive:0:ImageName string seed.iso' "$B/config.plist"
```

Start it with `utmctl start feedflow-flatpak`; cloud-init finishes in under a minute.

## 5. Desktop and tools (over SSH)

```bash
VM="$USER@feedflow-flatpak.local"
ssh "$VM" '
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y ubuntu-desktop-minimal flatpak flatpak-builder \
    xvfb x11-apps imagemagick git python3 spice-vdagent
  flatpak remote-add --user --if-not-exists flathub https://dl.flathub.org/repo/flathub.flatpakrepo
  sudo systemctl set-default graphical.target'
```

Auto-login without a password and no screen lock (the VM is only reachable from this Mac through
UTM's shared NAT network, and SSH accepts keys only):

```bash
ssh "$VM" '
  sudo sed -i -e "s/^#\? *AutomaticLoginEnable *=.*/AutomaticLoginEnable=True/" \
              -e "s/^#\? *AutomaticLogin *=.*/AutomaticLogin=$USER/" /etc/gdm3/custom.conf
  sudo passwd -d "$USER"
  sudo mkdir -p /etc/dconf/db/local.d /etc/dconf/profile
  printf "user-db:user\nsystem-db:local\n" | sudo tee /etc/dconf/profile/user
  printf "[org/gnome/desktop/screensaver]\nlock-enabled=false\n[org/gnome/desktop/session]\nidle-delay=uint32 0\n" \
    | sudo tee /etc/dconf/db/local.d/00-nolock
  sudo dconf update
  sudo reboot'
```

The first reboot into GNOME froze once (soft lockup, no SSH). Stopping and starting the VM with
`utmctl` fixed it, and it did not recur.

The first `build` installs the Flathub 25.08 SDK, Platform and the openjdk21/25 extensions, which
`generate` also uses.

## Gotchas

- macOS's built-in `rsync` (openrsync) hangs against the VM's GNU rsync; `sync` uses tar over SSH.
- Over SSH, GUI apps need the GNOME session's environment; use `systemd-run --user` (as `gui` does).
