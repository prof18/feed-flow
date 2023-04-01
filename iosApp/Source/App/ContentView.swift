import SwiftUI
import shared

struct ContentView: View {
    
    @EnvironmentObject var appState: AppState
    
    
    var body: some View {
        
        ZStack {
            NavigationStack {
                HomeScreen()
                    .environmentObject(appState)
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
                
                Snackbar(snackbarData: $appState.snackbarData)
            }
        }
    }
}
