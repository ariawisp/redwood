package dev.zed.redwood.schema.core

import app.cash.redwood.schema.Modifier
import app.cash.redwood.schema.Schema

@Schema(
  members = [
    ForegroundColor::class,
    BackgroundColor::class,
    TextStyle::class,
    HeadlineStyle::class,
    Padding::class,
    Gap::class,
    CornerRadius::class,
    Elevation::class,
    KeyBindingHint::class,
    MinimumSize::class,
    MaximumSize::class,
    Alignment::class,
  ],
)
public interface ZedCore

/** Semantic color roles aligned with Zed theme tokens. */
public enum class ColorRole {
  Default,
  Muted,
  Hidden,
  Placeholder,
  Accent,
  Created,
  Modified,
  Conflict,
  Ignored,
  Debugger,
  Deleted,
  Disabled,
  Hint,
  Info,
  Selected,
  Success,
  Warning,
  Error,
  VersionControlAdded,
  VersionControlModified,
  VersionControlDeleted,
  VersionControlIgnored,
  VersionControlConflict,
}

/** Optional parameterized color roles (players, custom HSLA). */
public data class ColorSupplement(
  val playerIndex: UInt? = null,
  val customColor: Hsla? = null,
)

public data class Hsla(
  val hue: Float,
  val saturation: Float,
  val lightness: Float,
  val alpha: Float,
)

/** Mapping of UI text sizes to semantic slots. */
public enum class TextStyleToken {
  Display,
  TitleLarge,
  Title,
  Body,
  BodyStrong,
  Caption,
  CaptionStrong,
  Mono,
}

public enum class TextEmphasis {
  Regular,
  Muted,
  Strong,
}

/** Density-aware spacing tokens derived from DynamicSpacing in GPUI. */
public enum class SpacingToken {
  None,
  Base1,
  Base2,
  Base3,
  Base4,
  Base6,
  Base8,
  Base10,
  Base12,
  Base16,
  Base20,
  Base24,
  Base32,
  Base40,
  Base48,
}

public enum class CornerStyle {
  Sharp,
  ExtraSmall,
  Small,
  Medium,
  Large,
  Pill,
}

public enum class ElevationLevel {
  Background,
  Surface,
  EditorSurface,
  ElevatedSurface,
  ModalSurface,
}

public enum class AlignmentAxis {
  Start,
  Center,
  End,
  Stretch,
}

public data class KeyChord(
  val keys: List<String>,
)

public enum class KeybindingDisplay {
  Inline,
  Trailing,
  Hidden,
}

public data class SizeConstraint(
  val width: Float? = null,
  val height: Float? = null,
)

@Modifier(1)
public data class ForegroundColor(
  val role: ColorRole,
  val supplement: ColorSupplement = ColorSupplement(),
)

@Modifier(2)
public data class BackgroundColor(
  val role: ColorRole,
  val supplement: ColorSupplement = ColorSupplement(),
)

@Modifier(10)
public data class TextStyle(
  val style: TextStyleToken = TextStyleToken.Body,
  val emphasis: TextEmphasis = TextEmphasis.Regular,
  val monospace: Boolean = false,
)

@Modifier(11)
public data class HeadlineStyle(
  val style: TextStyleToken = TextStyleToken.Title,
  val emphasis: TextEmphasis = TextEmphasis.Regular,
)

@Modifier(20)
public data class Padding(
  val start: SpacingToken = SpacingToken.None,
  val end: SpacingToken = SpacingToken.None,
  val top: SpacingToken = SpacingToken.None,
  val bottom: SpacingToken = SpacingToken.None,
)

@Modifier(21)
public data class Gap(
  val horizontal: SpacingToken = SpacingToken.None,
  val vertical: SpacingToken = SpacingToken.None,
)

@Modifier(25)
public data class CornerRadius(
  val style: CornerStyle = CornerStyle.Medium,
)

@Modifier(30)
public data class Elevation(
  val level: ElevationLevel = ElevationLevel.Surface,
)

@Modifier(40)
public data class KeyBindingHint(
  val chords: List<KeyChord>,
  val display: KeybindingDisplay = KeybindingDisplay.Trailing,
  val description: String? = null,
)

@Modifier(50)
public data class MinimumSize(
  val constraint: SizeConstraint,
)

@Modifier(51)
public data class MaximumSize(
  val constraint: SizeConstraint,
)

@Modifier(60)
public data class Alignment(
  val horizontal: AlignmentAxis = AlignmentAxis.Stretch,
  val vertical: AlignmentAxis = AlignmentAxis.Start,
)
