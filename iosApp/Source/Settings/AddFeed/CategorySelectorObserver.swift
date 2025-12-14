//
//  CategorySelectorObserver.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

@Observable
class CategorySelectorObserver {
    var selectedCategory: CategoriesState.CategoryItem?
    var onCategorySelected: ((String) -> Void)?

    func selectCategory(_ category: CategoriesState.CategoryItem) {
        selectedCategory = category
        onCategorySelected?(category.id)
    }
}
