//
//  CategorySelectorObserver.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/01/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import shared

class CategorySelectorObserver: ObservableObject {
    @Published var selectedCategory: CategoriesState.CategoryItem? {
        didSet {
            if let selectedCategory = selectedCategory {
                selectedCategory.onClick(CategoryId(value: selectedCategory.id))
            }
        }
    }
}
