package com.example

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.export.ExcelExporter
import com.example.model.SupportRecord
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Destek Çağrı Kayıt ve Excel Raporlama")
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun testExcelColumnsCount() {
    assertEquals(36, SupportRecord.EXCEL_COLUMNS.size)
    val sample = SupportRecord(redmineId1 = "RM-101", musteriBayiAdi = "Test A.Ş.")
    val values = sample.toExcelRowValues()
    assertEquals(36, values.size)

    val baos = ByteArrayOutputStream()
    ExcelExporter.writeXlsx(baos, listOf(sample))
    assertTrue(baos.size() > 100)
  }
}

