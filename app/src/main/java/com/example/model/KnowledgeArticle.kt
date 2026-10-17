package com.example.model

data class KnowledgeArticle(
    val id: String,
    val errorCode: String,
    val title: String,
    val category: String,
    val cause: String,
    val steps: List<String>,
    val recommendedTespit: String,
    val recommendedIslem: String
)
