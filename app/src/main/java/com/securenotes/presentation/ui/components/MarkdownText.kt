package com.securenotes.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Simple markdown text renderer supporting:
 * - **bold** text
 * - *italic* text
 * - # Headings
 * - - List items
 * - Numbered lists
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val lines = remember(text) { text.split("\n") }
    
    SelectionContainer(modifier = modifier) {
        Column {
            lines.forEachIndexed { index, line ->
                MarkdownLine(line = line)
                if (index < lines.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun MarkdownLine(line: String) {
    when {
        // Heading 1
        line.startsWith("# ") -> {
            Text(
                text = line.removePrefix("# "),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // Heading 2
        line.startsWith("## ") -> {
            Text(
                text = line.removePrefix("## "),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // Heading 3
        line.startsWith("### ") -> {
            Text(
                text = line.removePrefix("### "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // Bullet list
        line.startsWith("- ") || line.startsWith("* ") -> {
            Row(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                StyledText(
                    text = line.removePrefix("- ").removePrefix("* "),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        // Numbered list (1. 2. 3. etc.)
        line.matches(Regex("^\\d+\\.\\s.*")) -> {
            val number = line.substringBefore(".")
            val content = line.substringAfter(". ")
            Row(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = "$number.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                StyledText(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        // Regular text
        else -> {
            StyledText(
                text = line,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Renders inline styles: **bold**, *italic*, ***bold italic***
 */
@Composable
private fun StyledText(
    text: String,
    style: androidx.compose.ui.text.TextStyle
) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            var remaining = text
            
            while (remaining.isNotEmpty()) {
                when {
                    // Bold italic: ***text***
                    remaining.startsWith("***") -> {
                        val endIndex = remaining.indexOf("***", 3)
                        if (endIndex > 3) {
                            val boldItalicText = remaining.substring(3, endIndex)
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                                append(boldItalicText)
                            }
                            remaining = remaining.substring(endIndex + 3)
                        } else {
                            append("***")
                            remaining = remaining.substring(3)
                        }
                    }
                    // Bold: **text**
                    remaining.startsWith("**") -> {
                        val endIndex = remaining.indexOf("**", 2)
                        if (endIndex > 2) {
                            val boldText = remaining.substring(2, endIndex)
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(boldText)
                            }
                            remaining = remaining.substring(endIndex + 2)
                        } else {
                            append("**")
                            remaining = remaining.substring(2)
                        }
                    }
                    // Italic: *text*
                    remaining.startsWith("*") -> {
                        val endIndex = remaining.indexOf("*", 1)
                        if (endIndex > 1) {
                            val italicText = remaining.substring(1, endIndex)
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(italicText)
                            }
                            remaining = remaining.substring(endIndex + 1)
                        } else {
                            append("*")
                            remaining = remaining.substring(1)
                        }
                    }
                    // Regular character
                    else -> {
                        append(remaining.first())
                        remaining = remaining.substring(1)
                    }
                }
            }
        }
    }
    
    Text(
        text = annotatedString,
        style = style,
        color = MaterialTheme.colorScheme.onSurface
    )
}
