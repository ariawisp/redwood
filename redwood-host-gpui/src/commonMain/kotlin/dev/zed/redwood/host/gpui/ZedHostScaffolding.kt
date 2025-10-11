package dev.zed.redwood.host.gpui

/**
 * Placeholder metadata describing how the forthcoming generated Redwood → GPUI host bindings
 * should be wired. The real implementation will replace these lists with the generated widget
 * factories and modifier translators produced by redwood-codegen.
 */
object ZedHostScaffolding {
  /** Describes a single widget mapping from the Zed schema to an existing GPUI component. */
  data class WidgetPlan(
    val schema: String,
    val widgetTag: Int,
    val widgetName: String,
    val gpuiComponent: String,
    val notes: String,
  )

  /** Describes how a schema-level modifier should be translated into GPUI styling. */
  data class ModifierPlan(
    val schema: String,
    val modifierTag: Int,
    val modifierName: String,
    val gpuiApi: String,
    val notes: String,
  )

  /**
   * Widgets the new schemas introduce. Tag values mirror the allocations documented in
   * docs/redwood/schema-tags.md.
   */
  val widgetPlans = listOf(
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 1,
      widgetName = "Button",
      gpuiComponent = "ui::Button",
      notes = "Map style/emphasis to ButtonStyle/ButtonVariant; surface keybinding badges via Slot.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 2,
      widgetName = "IconButton",
      gpuiComponent = "ui::IconButton",
      notes = "Drive toggle state through ButtonLike::selected; expose accessibility label.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 3,
      widgetName = "ToggleButton",
      gpuiComponent = "ui::Toggle",
      notes = "Translate ToggleKind into switch/checkbox/icon flavours; route onToggle callbacks.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 20,
      widgetName = "Banner",
      gpuiComponent = "ui::Banner",
      notes = "Severity maps to TintColor; trailing actions slot becomes banner secondary content.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 21,
      widgetName = "Callout",
      gpuiComponent = "ui::Callout",
      notes = "CalloutStyle chooses accent/background pair; action slot populates trailing buttons.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 30,
      widgetName = "TabBar",
      gpuiComponent = "ui::TabBar",
      notes = "Variant/layout determine TabBar::style; children scope yields ui::Tab entries.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 31,
      widgetName = "Tab",
      gpuiComponent = "ui::Tab",
      notes = "Selected flag binds to Tab::selected; badge slot renders via Tab::badge.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 40,
      widgetName = "ExtensionCard",
      gpuiComponent = "extensions_ui::ExtensionCard",
      notes = "Leading/trailing/body slots map directly; overriddenByDevExtension toggles overlay.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 41,
      widgetName = "FeatureUpsell",
      gpuiComponent = "extensions_ui::FeatureUpsell",
      notes = "Docs URL drives button onClick; trailing content slot is injected via extend().",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 50,
      widgetName = "ListRow",
      gpuiComponent = "ui::ListItem",
      notes = "Accessory/selection data maps to ListItem API; click handler wires to on_activate.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 60,
      widgetName = "Menu",
      gpuiComponent = "ui::ContextMenu",
      notes = "Style selects ContextMenu vs DropdownMenu; item scope populates menu entries.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 61,
      widgetName = "MenuItem",
      gpuiComponent = "ui::ContextMenu::Item",
      notes = "Role toggles destructive/emphasized styling; shortcut text surfaces as key chord.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 70,
      widgetName = "Notification",
      gpuiComponent = "ui::Notification",
      notes = "Progress value maps to determinate bar; action slot populates inline buttons.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 80,
      widgetName = "Modal",
      gpuiComponent = "ui::Modal",
      notes = "Size drives ModalKind; header/body/footer slots map to builder-style methods.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.components.ZedComponents",
      widgetTag = 81,
      widgetName = "Popover",
      gpuiComponent = "ui::Popover",
      notes = "Placement/gutter feed Popover anchor; modal flag toggles dismiss-on-outside behaviour.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.layout.ZedLayout",
      widgetTag = 1,
      widgetName = "Stack",
      gpuiComponent = "ui::Stack",
      notes = "Content children become Stack::children; overlays pipe into Stack::overlay slot.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.layout.ZedLayout",
      widgetTag = 2,
      widgetName = "Surface",
      gpuiComponent = "ui::Group",
      notes = "Role/elevated toggle picks background/elevation tokens from ThemeSettings.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.layout.ZedLayout",
      widgetTag = 3,
      widgetName = "ScrollContainer",
      gpuiComponent = "ui::Scrollable",
      notes = "Axis drives ScrollDirection; showIndicators configures scrollbar visibility.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.layout.ZedLayout",
      widgetTag = 4,
      widgetName = "ItemList",
      gpuiComponent = "ui::List",
      notes = "Style toggles List::variant; itemSpacing binds to gap() modifier.",
    ),
    WidgetPlan(
      schema = "dev.zed.redwood.schema.layout.ZedLayout",
      widgetTag = 5,
      widgetName = "WorkspacePane",
      gpuiComponent = "workspace::WorkspaceItem",
      notes = "Bridges create_panel/apply plumbing to actual workspace pane hosting.",
    ),
  )

  /**
   * Modifiers from the core schema and how they map to existing GPUI styling helpers.
   */
  val modifierPlans = listOf(
    ModifierPlan(
      schema = "dev.zed.redwood.schema.core.ZedCore",
      modifierTag = 1,
      modifierName = "ForegroundColor",
      gpuiApi = "Styled::color(...)",
      notes = "Resolve ColorRole through ThemeSettings, falling back to custom HSLA when supplied.",
    ),
    ModifierPlan(
      schema = "dev.zed.redwood.schema.core.ZedCore",
      modifierTag = 2,
      modifierName = "BackgroundColor",
      gpuiApi = "Styled::bg(...)",
      notes = "Apply semantic surface backgrounds; ensure opacity matches elevation.",
    ),
    ModifierPlan(
      schema = "dev.zed.redwood.schema.core.ZedCore",
      modifierTag = 10,
      modifierName = "TextStyle",
      gpuiApi = "StyledTypography::*",
      notes = "Style + emphasis pick TextSize + font_weight; monospace toggles buffer font.",
    ),
    ModifierPlan(
      schema = "dev.zed.redwood.schema.core.ZedCore",
      modifierTag = 20,
      modifierName = "Padding",
      gpuiApi = "Styled::p_*",
      notes = "Translate SpacingToken via DynamicSpacing + UiDensity to px values.",
    ),
    ModifierPlan(
      schema = "dev.zed.redwood.schema.core.ZedCore",
      modifierTag = 25,
      modifierName = "CornerRadius",
      gpuiApi = "Styled::rounded_*",
      notes = "CornerStyle maps to radius tokens (sharp=0, pill=full, etc.).",
    ),
    ModifierPlan(
      schema = "dev.zed.redwood.schema.core.ZedCore",
      modifierTag = 30,
      modifierName = "Elevation",
      gpuiApi = "ElevationIndex.shadow()/bg()",
      notes = "Apply both background fill and box shadow to match Zed surfaces.",
    ),
    ModifierPlan(
      schema = "dev.zed.redwood.schema.core.ZedCore",
      modifierTag = 40,
      modifierName = "KeyBindingHint",
      gpuiApi = "ButtonLike::key_binding(...)",
      notes = "Render chords as KeyBinding components; display controls inline/trailing.",
    ),
    ModifierPlan(
      schema = "dev.zed.redwood.schema.core.ZedCore",
      modifierTag = 60,
      modifierName = "Alignment",
      gpuiApi = "Flex alignment helpers",
      notes = "Horizontal/vertical axes map to Styled alignment (justify/align_items).",
    ),
  )
}
