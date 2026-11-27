# Material 3 Expressive Adoption Guide

This document provides a detailed blueprint for adopting the latest **Google Material 3** and **Material 3 Expressive** features across your codebase.

---

## 1. Overview of Features to Adopt

| Feature | Target Component | Material 3 Expressive API | Goal |
| :--- | :--- | :--- | :--- |
| **Floating Toolbar** | Rich-Text & Selection Toolbars | `HorizontalFloatingToolbar`, `FloatingToolbarDefaults` | Replace custom popup containers with elevated, animated floating action docks. |
| **Split Button** | Action & Save Menus | `SplitButton`, `ToggleSplitButton` | Combine primary action buttons with connected drop-down menu arrows. |
| **Carousel** | Media Attachments | `HorizontalMultiBrowseCarousel` | Upgrade image lists to morphing, focus-scaling carousel lists. |
| **Wavy Slider** | Hour & Font Adjustment | `WavySlider` | Add dynamic wavy motion feedback during slider drag gestures. |
| **Morphing Loading Indicator** | Backup & Sync Loading | `LoadingIndicator` | Replace static spinners with shape-shifting vector loading animations. |
| **Adaptive Navigation** | Root Navigation | `NavigationSuiteScaffold`, `WideNavigationRail` | Adapt navigation layout automatically across Phone, Foldable, and Tablet screens. |

---

## 2. File-by-File Adoption Plan

### File 1: [`FormatPanel.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/editor/components/FormatPanel.kt) & [`SelectionToolbar.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/editor/components/SelectionToolbar.kt)

#### Target Area
The rich text formatting panel and floating selection toolbar in the note editor.

#### What Needs to Change
Replace custom background `Surface` and `Popup` implementations with M3 Expressive `HorizontalFloatingToolbar`.

#### Code Blueprint
```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveFormatToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    HorizontalFloatingToolbar(
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        content = content
    )
}
```

---

### File 2: [`NoteEditorScreen.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/notes/NoteEditorScreen.kt) & [`BackupScreen.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/settings/BackupScreen.kt)

#### Target Area
Save actions, export options, and manual backup buttons.

#### What Needs to Change
Convert standalone `Button` + `DropdownMenu` combinations into official M3 Expressive `SplitButton`.

#### Code Blueprint
```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SplitButton
import androidx.compose.material3.SplitButtonDefaults

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SaveSplitButton(
    onSave: () -> Unit,
    onExpandMenu: () -> Unit,
) {
    SplitButton(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                onClick = onExpandMenu,
                expanded = false
            ) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "More Save Options")
            }
        }
    )
}
```

---

### File 3: [`AttachedImage.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/editor/components/AttachedImage.kt)

#### Target Area
Inline note image attachment browser row.

#### What Needs to Change
Replace `LazyRow` with `HorizontalMultiBrowseCarousel` for expressive shape morphing between items.

#### Code Blueprint
```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachedImagesCarousel(
    images: List<String>,
    onImageClick: (String) -> Unit
) {
    val carouselState = rememberCarouselState { images.size }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 180.dp,
        itemSpacing = 8.dp,
    ) { index ->
        val imageName = images[index]
        ImageCard(name = imageName, onClick = { onImageClick(imageName) })
    }
}
```

---

### File 4: [`AddHoursSheet.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/home/AddHoursSheet.kt)

#### Target Area
Time entry / quick-add hour slider controls.

#### What Needs to Change
Replace standard `Slider` with `WavySlider` to give tactile visual motion when dragging.

#### Code Blueprint
```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.WavySlider

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveHourSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedRange<Float>
) {
    WavySlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange
    )
}
```

---

### File 5: [`BackupScreen.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/settings/BackupScreen.kt)

#### Target Area
Cloud Drive backup and local export running state headers.

#### What Needs to Change
Upgrade `CircularProgressIndicator` during indeterminate loading to M3 Expressive morphing `LoadingIndicator`.

#### Code Blueprint
```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSyncingSpinner() {
    LoadingIndicator(
        modifier = Modifier.size(48.dp)
    )
}
```

---

### File 6: [`MainActivity.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/MainActivity.kt)

#### Target Area
Bottom navigation scaffold (`AppBottomNavBar`).

#### What Needs to Change
Wrap `MainAppScaffold` navigation in `NavigationSuiteScaffold` so the app automatically presents a `WideNavigationRail` on foldables and tablets while maintaining `ShortNavigationBar` on mobile screens.

#### Code Blueprint
```kotlin
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold

@Composable
fun MainAdaptiveScaffold(
    currentDestination: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            // define items dynamically
        },
        content = content
    )
}
```

---

## 3. Recommended Implementation Order

1. **Step 1:** Adopt `LoadingIndicator` in [`BackupScreen.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/settings/BackupScreen.kt) (Quickest visual enhancement).
2. **Step 2:** Adopt `WavySlider` in [`AddHoursSheet.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/home/AddHoursSheet.kt).
3. **Step 3:** Adopt `SplitButton` in [`NoteEditorScreen.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/notes/NoteEditorScreen.kt).
4. **Step 4:** Adopt `HorizontalFloatingToolbar` in [`FormatPanel.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/editor/components/FormatPanel.kt).
5. **Step 5:** Adopt `HorizontalMultiBrowseCarousel` in [`AttachedImage.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/ui/editor/components/AttachedImage.kt).
6. **Step 6:** Adopt `NavigationSuiteScaffold` in [`MainActivity.kt`](file:///Users/user/AndroidStudioProjects/MyApplication/app/src/main/java/com/harsha/myapplication/MainActivity.kt).
