import SwiftUI
import shared // KMP module

struct BlockedWordsView: View {
    // Using VMStoreOwner, similar to SettingsScreen.swift
    // Assuming Deps.shared.getBlockedWordsViewModel() is the way to get the ViewModel instance.
    @StateObject private var vmStoreOwner = VMStoreOwner<BlockedWordsViewModel>(
        Deps.shared.getBlockedWordsViewModel() // This needs to be the actual way Koin provides the ViewModel
    )

    // Local state mirroring the KMP ViewModel's UiState
    @State private var uiState: BlockedWordsUiState = BlockedWordsUiState(
        blockedWords: [],
        isLoading: true,
        error: nil,
        newWordText: ""
    )
    
    // Accessing strings
    private let strings = feedFlowStrings

    var body: some View {
        VStack {
            // Input Area
            HStack {
                TextField(
                    strings.blockedWordsInputPlaceholder,
                    text: Binding(
                        get: { uiState.newWordText },
                        set: { newValue in
                            // Update local state immediately for responsiveness if needed,
                            // but primarily forward to ViewModel.
                            // ViewModel's state flow should then update uiState.newWordText.
                            vmStoreOwner.instance.updateNewWordText(text: newValue)
                        }
                    )
                )
                .textFieldStyle(RoundedBorderTextFieldStyle())
                
                Button(strings.blockedWordsAddButton) {
                    vmStoreOwner.instance.addBlockedWord()
                    // The ViewModel should clear its newWordText, which will flow back to uiState.
                }
                // Disable button if the text directly controlled by ViewModel state is empty.
                .disabled(uiState.newWordText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding()

            // Content Area
            if uiState.isLoading {
                ProgressView()
                Spacer()
            } else if let errorMsg = uiState.error {
                Text(errorMsg)
                    .foregroundColor(.red)
                    .padding()
                Spacer()
            } else if uiState.blockedWords.isEmpty {
                Text(strings.blockedWordsEmptyList)
                    .padding()
                Spacer()
            } else {
                List {
                    ForEach(uiState.blockedWords, id: \.self) { word in
                        Text(word)
                    }
                    .onDelete(perform: deleteWord)
                }
            }
        }
        .navigationTitle(strings.blockedWordsTitle)
        .task {
            // Collect from the ViewModel's StateFlow to update local @State
            // Ensure that uiState from KMP is correctly cast/bridged.
            // If KMP-NativeCoroutines is used, this would be straightforward.
            // Otherwise, a custom bridging mechanism or SKIE is needed.
            // For this task, assuming `vmStoreOwner.instance.uiState` is an AsyncSequence<BlockedWordsUiState>.
            // This requires the KMP ViewModel's StateFlow to be exposed as such to Swift.
            // (e.g. via @CommonFlow from KMP-NativeCoroutines or SKIE generation)
            for await kmpUiState in vmStoreOwner.instance.uiState {
                self.uiState = kmpUiState
            }
        }
        // .onDisappear { // If VMStoreOwner doesn't handle onCleared automatically via its own deinit
        //    vmStoreOwner.instance.onCleared()
        // }
    }

    private func deleteWord(at offsets: IndexSet) {
        offsets.forEach { index in
            // Ensure index is valid before accessing.
            // uiState.blockedWords should be up-to-date from the .task collector.
            if index < uiState.blockedWords.count {
                let word = uiState.blockedWords[index]
                vmStoreOwner.instance.deleteBlockedWord(word: word)
            }
        }
    }
}

// Preview might need adjustment depending on how Deps and Koin are set up for previews.
// For now, keeping it simple. It might not fully work if Deps.shared relies on a running app.
struct BlockedWordsView_Previews: PreviewProvider {
    static var previews: some View {
        // This preview setup might require providing a mock/stubbed ViewModel
        // if direct Koin DI is problematic in Xcode Previews.
        // For the purpose of this task, we assume it can be constructed.
        NavigationView {
            BlockedWordsView()
        }
    }
}
