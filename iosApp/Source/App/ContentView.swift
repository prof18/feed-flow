import SwiftUI
import shared

struct ContentView: View {
    
    @EnvironmentObject var appState: AppState
    @EnvironmentObject var browserSelector: BrowserSelector
    
    
    var body: some View {
        
        ZStack {
            NavigationStack {
                HomeScreen()
                    .environmentObject(appState)
                    .environmentObject(browserSelector)
                    .navigationDestination(for: Route.self) { route in
                        //                switch route {
                        //                case let .movieDetail(movieId):
                        //                    MovieDetailScreen(
                        //                        movieDetailState: viewModel.movieDetailState,
                        //                        onAppear: {
                        //                            viewModel.getMovie(movieId: movieId)
                        //                        }
                        //                    )
                        //                }
                    }
            }
            
            VStack(spacing: 0) {
                
                Spacer()
                
                Snackbar(messageQueue: $appState.snackbarQueue)
            }
        }
    }
}
