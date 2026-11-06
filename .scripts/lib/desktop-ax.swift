// Reads and presses UI elements of the running Desktop app through the macOS Accessibility API,
// including dialog windows the Compose Hot Reload MCP cannot see. Works in the background: the app is
// not brought to the front. Run it through .scripts/desktop-ax.sh.
import ApplicationServices
import Foundation

let args = CommandLine.arguments
guard args.count >= 4, let pid = pid_t(args[1]) else {
    FileHandle.standardError.write(Data("usage: desktop-ax <pid> <window> dump | press [AXRole:]<label>\n".utf8))
    exit(2)
}
let windowQuery = args[2].lowercased()
let command = args[3]

func attr(_ element: AXUIElement, _ name: String) -> AnyObject? {
    var value: AnyObject?
    return AXUIElementCopyAttributeValue(element, name as CFString, &value) == .success ? value : nil
}

func string(_ element: AXUIElement, _ name: String) -> String {
    (attr(element, name) as? String) ?? ""
}

func children(_ element: AXUIElement) -> [AXUIElement] {
    (attr(element, kAXChildrenAttribute) as? [AXUIElement]) ?? []
}

func label(_ element: AXUIElement) -> String {
    [string(element, kAXTitleAttribute), string(element, kAXDescriptionAttribute), string(element, kAXValueAttribute)]
        .filter { !$0.isEmpty }
        .joined(separator: " | ")
}

let app = AXUIElementCreateApplication(pid)
AXUIElementSetAttributeValue(app, "AXManualAccessibility" as CFString, kCFBooleanTrue)
let windows = (attr(app, kAXWindowsAttribute) as? [AXUIElement]) ?? []
guard let window = windows.first(where: { string($0, kAXTitleAttribute).lowercased().contains(windowQuery) }) else {
    print("no window matching '\(args[2])'; windows: \(windows.map { string($0, kAXTitleAttribute) })")
    exit(1)
}

var elements: [(element: AXUIElement, depth: Int)] = []
func walk(_ element: AXUIElement, _ depth: Int) {
    elements.append((element, depth))
    if depth < 60 { children(element).forEach { walk($0, depth + 1) } }
}
walk(window, 0)

switch command {
case "dump":
    for (element, depth) in elements {
        let role = string(element, kAXRoleAttribute)
        let text = label(element)
        if !text.isEmpty || ["AXButton", "AXTextField", "AXTextArea", "AXCheckBox"].contains(role) {
            print(String(repeating: "  ", count: depth) + role + "  " + text)
        }
    }
case "press":
    // "AXButton:Cancel" limits the match to one role; an exact label wins over a partial one.
    var target = args.dropFirst(4).joined(separator: " ")
    var role: String?
    if target.hasPrefix("AX"), let colon = target.firstIndex(of: ":") {
        role = String(target[..<colon])
        target = String(target[target.index(after: colon)...])
    }
    target = target.lowercased()
    let candidates = elements.filter { role == nil || string($0.element, kAXRoleAttribute) == role }
    let exact = candidates.first { candidate in
        label(candidate.element).lowercased().split(separator: "|")
            .contains { $0.trimmingCharacters(in: .whitespaces) == target }
    }
    guard let match = exact ?? candidates.first(where: { label($0.element).lowercased().contains(target) }) else {
        print("nothing labelled '\(target)'")
        exit(1)
    }
    let result = AXUIElementPerformAction(match.element, kAXPressAction as CFString)
    if result == .success {
        print("pressed \(label(match.element))")
    } else {
        print("press failed: \(result.rawValue)")
        exit(1)
    }
default:
    print("unknown command \(command)")
    exit(2)
}
