package dev.zed.redwood.schema.components

import app.cash.redwood.layout.RedwoodLayout
import app.cash.redwood.schema.Children
import app.cash.redwood.schema.Property
import app.cash.redwood.schema.Schema
import app.cash.redwood.schema.Schema.Dependency
import app.cash.redwood.schema.Widget
import app.cash.redwood.ui.basic.RedwoodUiBasic
import dev.zed.redwood.schema.core.AlignmentAxis
import dev.zed.redwood.schema.core.ColorRole
import dev.zed.redwood.schema.core.SpacingToken
import dev.zed.redwood.schema.layout.ZedLayout

@Schema(
  members = [
    Button::class,
    IconButton::class,
    ToggleButton::class,
    Badge::class,
    Banner::class,
    Callout::class,
    TabBar::class,
    Tab::class,
    ExtensionCard::class,
    FeatureUpsell::class,
    ListRow::class,
    Menu::class,
    MenuItem::class,
    Notification::class,
    Modal::class,
    Popover::class,
  ],
  dependencies = [
    Dependency(1, RedwoodUiBasic::class),
    Dependency(2, RedwoodLayout::class),
    Dependency(3, ZedLayout::class),
  ],
)
public interface ZedComponents

public enum class ButtonStyle {
  Neutral,
  Accent,
  Danger,
  Success,
}

public enum class ButtonEmphasis {
  Solid,
  Subtle,
  Ghost,
  Outline,
}

public data class IconToken(
  val name: String,
  val size: IconSize = IconSize.Regular,
)

public enum class IconSize {
  Small,
  Regular,
  Large,
}

public enum class BannerSeverity {
  Info,
  Success,
  Warning,
  Error,
}

public enum class CalloutStyle {
  Neutral,
  Success,
  Warning,
  Critical,
}

public enum class BadgeStyle {
  Neutral,
  Accent,
  Success,
  Warning,
  Danger,
}

public enum class TabBarVariant {
  Segmented,
  Underline,
  Sidebar,
}

public enum class TabLayout {
  Horizontal,
  Vertical,
}

public enum class MenuStyle {
  Contextual,
  Dropdown,
  Toolbar,
}

public enum class MenuItemRole {
  Default,
  Destructive,
  Emphasized,
}

public enum class NotificationStyle {
  Info,
  Success,
  Warning,
  Error,
  Progress,
}

public enum class ModalSize {
  Small,
  Medium,
  Large,
  Full,
}

public enum class PopoverPlacement {
  Top,
  Bottom,
  Leading,
  Trailing,
  Auto,
}

public data class ButtonAccessory(
  val leadingIcon: IconToken? = null,
  val trailingIcon: IconToken? = null,
  val trailingBadge: BadgeContent? = null,
)

public data class BadgeContent(
  val text: String? = null,
  val style: BadgeStyle = BadgeStyle.Neutral,
)

public enum class ToggleKind {
  Checkbox,
  Switch,
  Icon,
}

public enum class RowAccessoryAlignment {
  Start,
  Center,
  End,
}

@Widget(1)
public data class Button(
  @Property(1) val label: String,
  @Property(2) val style: ButtonStyle = ButtonStyle.Neutral,
  @Property(3) val emphasis: ButtonEmphasis = ButtonEmphasis.Solid,
  @Property(4) val accessory: ButtonAccessory = ButtonAccessory(),
  @Property(5) val enabled: Boolean = true,
  @Property(6) val onClick: (() -> Unit)? = null,
)

@Widget(2)
public data class IconButton(
  @Property(1) val icon: IconToken,
  @Property(2) val accessibilityLabel: String,
  @Property(3) val style: ButtonStyle = ButtonStyle.Neutral,
  @Property(4) val emphasis: ButtonEmphasis = ButtonEmphasis.Ghost,
  @Property(5) val toggled: Boolean = false,
  @Property(6) val onClick: (() -> Unit)? = null,
)

@Widget(3)
public data class ToggleButton(
  @Property(1) val label: String,
  @Property(2) val kind: ToggleKind = ToggleKind.Switch,
  @Property(3) val checked: Boolean,
  @Property(4) val enabled: Boolean = true,
  @Property(5) val onToggle: ((Boolean) -> Unit)? = null,
)

@Widget(10)
public data class Badge(
  @Property(1) val content: BadgeContent,
)

@Widget(20)
public data class Banner(
  @Property(1) val title: String,
  @Property(2) val message: String? = null,
  @Property(3) val severity: BannerSeverity = BannerSeverity.Info,
  @Property(4) val icon: IconToken? = null,
  @Property(5) val primaryAction: ButtonAccessory? = null,
  @Property(6) val dismissible: Boolean = false,
  @Property(7) val onDismiss: (() -> Unit)? = null,
  @Children(1) val trailingContent: () -> Unit = {},
)

@Widget(21)
public data class Callout(
  @Property(1) val title: String,
  @Property(2) val message: String,
  @Property(3) val style: CalloutStyle = CalloutStyle.Neutral,
  @Property(4) val icon: IconToken? = null,
  @Property(5) val dismissible: Boolean = false,
  @Property(6) val onDismiss: (() -> Unit)? = null,
  @Children(1) val actions: () -> Unit = {},
)

object TabBarScope

@Widget(30)
public data class TabBar(
  @Property(1) val variant: TabBarVariant = TabBarVariant.Underline,
  @Property(2) val layout: TabLayout = TabLayout.Horizontal,
  @Property(3) val gap: SpacingToken = SpacingToken.Base4,
  @Children(1) val tabs: TabBarScope.() -> Unit,
)

@Widget(31)
public data class Tab(
  @Property(1) val title: String,
  @Property(2) val icon: IconToken? = null,
  @Property(3) val selected: Boolean = false,
  @Property(4) val badge: BadgeContent? = null,
  @Property(5) val onSelect: (() -> Unit)? = null,
  @Children(1) val content: () -> Unit = {},
)

@Widget(40)
public data class ExtensionCard(
  @Property(1) val title: String,
  @Property(2) val subtitle: String? = null,
  @Property(3) val installed: Boolean = false,
  @Property(4) val overriddenByDevExtension: Boolean = false,
  @Children(1) val leading: () -> Unit = {},
  @Children(2) val trailingActions: () -> Unit = {},
  @Children(3) val body: () -> Unit = {},
)

@Widget(41)
public data class FeatureUpsell(
  @Property(1) val message: String,
  @Property(2) val docsUrl: String? = null,
  @Children(1) val trailingContent: () -> Unit = {},
)

@Widget(50)
public data class ListRow(
  @Property(1) val title: String,
  @Property(2) val subtitle: String? = null,
  @Property(3) val selected: Boolean = false,
  @Property(4) val selectionColor: ColorRole = ColorRole.Accent,
  @Property(5) val supportingText: String? = null,
  @Property(6) val leadingAccessoryAlignment: RowAccessoryAlignment = RowAccessoryAlignment.Center,
  @Property(7) val trailingAccessoryAlignment: RowAccessoryAlignment = RowAccessoryAlignment.Center,
  @Property(8) val contentAlignment: AlignmentAxis = AlignmentAxis.Stretch,
  @Children(1) val leadingAccessory: () -> Unit = {},
  @Children(2) val trailingAccessory: () -> Unit = {},
  @Children(3) val bodyContent: () -> Unit = {},
  @Property(9) val onClick: (() -> Unit)? = null,
)

object MenuScope

@Widget(60)
public data class Menu(
  @Property(1) val style: MenuStyle = MenuStyle.Contextual,
  @Property(2) val title: String? = null,
  @Property(3) val maxHeight: Float? = null,
  @Children(1) val items: MenuScope.() -> Unit,
)

@Widget(61)
public data class MenuItem(
  @Property(1) val text: String,
  @Property(2) val shortcut: String? = null,
  @Property(3) val enabled: Boolean = true,
  @Property(4) val role: MenuItemRole = MenuItemRole.Default,
  @Property(5) val selected: Boolean = false,
  @Property(6) val onSelect: (() -> Unit)? = null,
  @Children(1) val trailingContent: () -> Unit = {},
)

@Widget(70)
public data class Notification(
  @Property(1) val title: String,
  @Property(2) val message: String? = null,
  @Property(3) val style: NotificationStyle = NotificationStyle.Info,
  @Property(4) val progress: Float? = null,
  @Property(5) val dismissible: Boolean = false,
  @Property(6) val onDismiss: (() -> Unit)? = null,
  @Children(1) val actions: () -> Unit = {},
)

@Widget(80)
public data class Modal(
  @Property(1) val title: String? = null,
  @Property(2) val size: ModalSize = ModalSize.Medium,
  @Property(3) val dismissible: Boolean = true,
  @Property(4) val onDismiss: (() -> Unit)? = null,
  @Children(1) val headerContent: () -> Unit = {},
  @Children(2) val body: () -> Unit,
  @Children(3) val footer: () -> Unit = {},
)

@Widget(81)
public data class Popover(
  @Property(1) val placement: PopoverPlacement = PopoverPlacement.Auto,
  @Property(2) val gutter: SpacingToken = SpacingToken.Base2,
  @Property(3) val modal: Boolean = false,
  @Children(1) val trigger: () -> Unit,
  @Children(2) val content: () -> Unit,
)
