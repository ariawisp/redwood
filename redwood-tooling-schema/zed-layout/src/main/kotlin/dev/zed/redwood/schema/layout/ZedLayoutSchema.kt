package dev.zed.redwood.schema.layout

import app.cash.redwood.layout.RedwoodLayout
import app.cash.redwood.schema.Children
import app.cash.redwood.schema.Property
import app.cash.redwood.schema.Schema
import app.cash.redwood.schema.Schema.Dependency
import app.cash.redwood.schema.Widget
import dev.zed.redwood.schema.core.SpacingToken

@Schema(
  members = [
    Stack::class,
    Surface::class,
    ScrollContainer::class,
    ItemList::class,
    WorkspacePane::class,
  ],
  dependencies = [
    Dependency(1, RedwoodLayout::class),
  ],
)
public interface ZedLayout

public enum class StackAlignment {
  TopStart,
  TopCenter,
  TopEnd,
  CenterStart,
  Center,
  CenterEnd,
  BottomStart,
  BottomCenter,
  BottomEnd,
}

public enum class SurfaceRole {
  Background,
  Primary,
  Elevated,
  Modal,
}

public enum class ScrollAxis {
  Vertical,
  Horizontal,
  Both,
}

public enum class ListStyle {
  Plain,
  Card,
  Striped,
}

@Widget(1)
public data class Stack(
  @Property(1) val alignment: StackAlignment = StackAlignment.TopStart,
  @Property(2) val spacing: SpacingToken = SpacingToken.None,
  @Children(1) val content: () -> Unit,
  @Children(2) val overlays: () -> Unit = {},
)

@Widget(2)
public data class Surface(
  @Property(1) val role: SurfaceRole = SurfaceRole.Primary,
  @Property(2) val elevated: Boolean = false,
  @Children(1) val content: () -> Unit,
)

@Widget(3)
public data class ScrollContainer(
  @Property(1) val axis: ScrollAxis = ScrollAxis.Vertical,
  @Property(2) val showIndicators: Boolean = true,
  @Children(1) val content: () -> Unit,
)

@Widget(4)
public data class ItemList(
  @Property(1) val style: ListStyle = ListStyle.Plain,
  @Property(2) val itemSpacing: SpacingToken = SpacingToken.Base4,
  @Children(1) val items: () -> Unit,
)

@Widget(5)
public data class WorkspacePane(
  @Property(1) val id: String,
  @Property(2) val title: String,
  @Property(3) val showChrome: Boolean = true,
  @Children(1) val content: () -> Unit,
)
