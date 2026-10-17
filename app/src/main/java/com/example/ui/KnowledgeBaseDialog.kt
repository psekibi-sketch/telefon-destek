package com.example.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.KnowledgeArticle
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper
import java.util.Locale

@Composable
fun KnowledgeBaseDialog(
    viewModel: SupportViewModel,
    onDismiss: () -> Unit
) {
    val articles by viewModel.knowledgeArticles.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("TÜMÜ") }
    var expandedArticleId by remember { mutableStateOf<String?>(null) }

    // Edit/Add dialog states
    var isAddEditDialogOpen by remember { mutableStateOf(false) }
    var editingArticle by remember { mutableStateOf<KnowledgeArticle?>(null) }
    var editErrorCode by remember { mutableStateOf("") }
    var editTitle by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Genel") }
    var editCause by remember { mutableStateOf("") }
    var editStepsRaw by remember { mutableStateOf("") }
    var editTespit by remember { mutableStateOf("") }
    var editIslem by remember { mutableStateOf("") }

    var articleToDelete by remember { mutableStateOf<KnowledgeArticle?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val categories = remember(articles) {
        listOf("TÜMÜ") + articles.map { it.category.trim() }.filter { it.isNotBlank() }.distinct()
    }

    val filteredArticles = remember(articles, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase(Locale("tr", "TR"))
        articles.filter { art ->
            val matchesCategory = selectedCategory == "TÜMÜ" || art.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = q.isBlank() ||
                    art.errorCode.lowercase(Locale("tr", "TR")).contains(q) ||
                    art.title.lowercase(Locale("tr", "TR")).contains(q) ||
                    art.cause.lowercase(Locale("tr", "TR")).contains(q) ||
                    art.steps.any { it.lowercase(Locale("tr", "TR")).contains(q) } ||
                    art.category.lowercase(Locale("tr", "TR")).contains(q)
            matchesCategory && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .imePadding()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2E103E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = iOSPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Arıza & Hata Kodu Rehberi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSTextPrimary
                            )
                            Text(
                                text = "Bilgi Bankası • ${articles.size} Çözüm Rehberi",
                                fontSize = 11.sp,
                                color = iOSTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = iOSTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Hata kodu (304, 500, 24 vb.) veya arıza konusu ara...", fontSize = 11.5.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = iOSPurple, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, color = iOSTextPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    colors = formTextFieldColors()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Category Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) iOSPurple else iOSCardSecondary,
                            border = BorderStroke(0.6.dp, if (isSelected) iOSPurple else iOSBorder),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else iOSTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions: Add New Article & Reset to Defaults
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bulunan: ${filteredArticles.size} Rehber",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = iOSTextSecondary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = ExcelGreenDark.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.6.dp, ExcelGreen),
                            modifier = Modifier.clickable {
                                editingArticle = null
                                editErrorCode = ""
                                editTitle = ""
                                editCategory = "Genel"
                                editCause = ""
                                editStepsRaw = ""
                                editTespit = ""
                                editIslem = ""
                                isAddEditDialogOpen = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Yeni Madde", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = ExcelGreen)
                            }
                        }

                        Surface(
                            color = iOSOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.6.dp, iOSOrange.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { showResetConfirm = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = iOSOrange, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Varsayılan", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = iOSOrange)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Articles List
                if (filteredArticles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = iOSTextTertiary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Aramanıza uygun arıza rehberi bulunamadı.", fontSize = 12.sp, color = iOSTextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredArticles, key = { it.id }) { article ->
                            val isExpanded = expandedArticleId == article.id
                            KnowledgeArticleCard(
                                article = article,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedArticleId = if (isExpanded) null else article.id
                                },
                                onApplyToForm = {
                                    viewModel.applyKnowledgeArticleToForm(article)
                                    onDismiss()
                                },
                                onSendWhatsApp = {
                                    viewModel.shareKnowledgeArticleViaWhatsApp(context, article)
                                },
                                onCopySteps = {
                                    val text = WhatsAppHelper.generateKnowledgeArticleInstructionText(article)
                                    clipboardManager.setText(AnnotatedString(text))
                                },
                                onEdit = {
                                    editingArticle = article
                                    editErrorCode = article.errorCode
                                    editTitle = article.title
                                    editCategory = article.category
                                    editCause = article.cause
                                    editStepsRaw = article.steps.joinToString("\n")
                                    editTespit = article.recommendedTespit
                                    editIslem = article.recommendedIslem
                                    isAddEditDialogOpen = true
                                },
                                onDelete = { articleToDelete = article }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = iOSPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Add / Edit Dialog
    if (isAddEditDialogOpen) {
        val isEditing = editingArticle != null
        AlertDialog(
            onDismissRequest = { isAddEditDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = iOSPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "Rehber Maddesini Düzenle" else "Yeni Arıza Rehberi Ekle",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editErrorCode,
                            onValueChange = { editErrorCode = it },
                            label = { Text("Hata Kodu (Örn: ERR-304)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f),
                            colors = formTextFieldColors()
                        )
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text("Kategori") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f),
                            colors = formTextFieldColors()
                        )
                    }

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Arıza / Başlık") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )

                    OutlinedTextField(
                        value = editCause,
                        onValueChange = { editCause = it },
                        label = { Text("Neden Kaynaklanır? (Olası Sebep)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )

                    OutlinedTextField(
                        value = editStepsRaw,
                        onValueChange = { editStepsRaw = it },
                        label = { Text("Çözüm Adımları (Her satıra bir adım yazın)") },
                        placeholder = { Text("1. İnternet kablosunu kontrol edin\n2. Cihazı kapatıp açın\n3. Test fişi kesin") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )

                    OutlinedTextField(
                        value = editTespit,
                        onValueChange = { editTespit = it },
                        label = { Text("Önerilen Tespit (Forma otomatik aktarılır)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )

                    OutlinedTextField(
                        value = editIslem,
                        onValueChange = { editIslem = it },
                        label = { Text("Önerilen İşlem (Forma otomatik aktarılır)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val steps = editStepsRaw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                        if (editTitle.isNotBlank()) {
                            if (isEditing) {
                                viewModel.updateKnowledgeArticle(
                                    id = editingArticle!!.id,
                                    errorCode = editErrorCode,
                                    title = editTitle,
                                    category = editCategory,
                                    cause = editCause,
                                    steps = steps,
                                    recommendedTespit = editTespit,
                                    recommendedIslem = editIslem
                                )
                            } else {
                                viewModel.addKnowledgeArticle(
                                    errorCode = editErrorCode,
                                    title = editTitle,
                                    category = editCategory,
                                    cause = editCause,
                                    steps = steps,
                                    recommendedTespit = editTespit,
                                    recommendedIslem = editIslem
                                )
                            }
                            isAddEditDialogOpen = false
                        }
                    },
                    enabled = editTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen)
                ) {
                    Text(if (isEditing) "Güncelle" else "Kaydet", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { isAddEditDialogOpen = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Delete Confirm Dialog
    articleToDelete?.let { art ->
        AlertDialog(
            onDismissRequest = { articleToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rehber Maddesini Sil", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "'${art.title}' rehber maddesi silinsin mi?",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteKnowledgeArticle(art.id)
                        articleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { articleToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = iOSOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Varsayılan Rehbere Dön", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Tüm arıza rehberi maddeleri fabrika çıkış hazır POS/ÖKC hata kodlarına sıfırlanacaktır. Devam etmek istiyor musunuz?",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDefaultKnowledgeArticles()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSOrange)
                ) {
                    Text("Sıfırla", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirm = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
fun KnowledgeArticleCard(
    article: KnowledgeArticle,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onApplyToForm: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onCopySteps: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.6.dp, if (isExpanded) iOSPurple.copy(alpha = 0.8f) else iOSBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Error Code & Category & Toggle Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (article.errorCode.isNotBlank()) {
                        Surface(
                            color = Color(0xFF2E103E),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, iOSPurple)
                        ) {
                            Text(
                                text = article.errorCode,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = iOSPurple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Surface(
                        color = iOSTeal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = article.category,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iOSTeal,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = iOSBlue, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = AccentRed, modifier = Modifier.size(15.dp))
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = iOSTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = article.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = iOSTextPrimary
            )

            // Cause (Always visible or short)
            if (article.cause.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text("🔍 ", fontSize = 11.sp)
                    Text(
                        text = "Sebep: ${article.cause}",
                        fontSize = 11.sp,
                        color = iOSTextSecondary,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Expanded Steps Section
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = iOSBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "📝 Adım Adım Çözüm Yolu:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSOrange
                )
                Spacer(modifier = Modifier.height(4.dp))

                article.steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = iOSPurple.copy(alpha = 0.2f),
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${idx + 1}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iOSPurple
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = step,
                            fontSize = 11.5.sp,
                            color = iOSTextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Row (Forma Aktar, WhatsApp ile Gönder, Kopyala)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onApplyToForm,
                        colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f).height(34.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Forma Aktar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSendWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f).height(34.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onCopySteps,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.9f).height(34.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kopyala", fontSize = 10.5.sp)
                    }
                }
            }
        }
    }
}
