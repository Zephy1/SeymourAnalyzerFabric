package schnerry.seymouranalyzer.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import schnerry.seymouranalyzer.gui.ModScreen;

import java.util.ArrayList;
import java.util.List;
import kotlin.math.max
import kotlin.math.min

/**
 * Custom GUI screen for editing match priorities with drag-and-drop reordering
 */
class PriorityEditorScreen(parent: Screen) : ModScreen(Component.literal("Match Priority Editor"), parent) {
    private val priorities: MutableList<MatchPriority> = ClothConfig.getInstance().matchPriorities.toMutableList()

    private var draggedIndex = -1
    private var hoveredIndex = -1
    private var dragStartY = 0.0
    private var currentDragY = 0.0

    companion object {
        private const val ITEM_HEIGHT = 30
        private const val ITEM_SPACING = 2
        private const val LIST_WIDTH = 400
        private const val LIST_START_Y = 60
    }

    override fun init() {
        super.init()

        // Done button
        addRenderableWidget(
            Button.builder(Component.literal("Done")) {
                ClothConfig.getInstance().matchPriorities = priorities
                ClothConfig.getInstance().save()
                close()
            }.bounds(width / 2 - 155, height - 28, 150, 20).build()
        )

        // Cancel button
        addRenderableWidget(
            Button.builder(Component.literal("Cancel")) { close() }
                .bounds(width / 2 + 5, height - 28, 150, 20).build()
        )

        // Reset to defaults button
        addRenderableWidget(
            Button.builder(Component.literal("Reset to Defaults")) {
                priorities.clear()
                priorities.addAll(ClothConfig.getDefaultMatchPriorities())
            }.bounds(width / 2 - 75, height - 52, 150, 20).build()
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // Title
        context.drawCenteredString(font, title, width / 2, 15, 0xFFFFFFFF.toInt())

        // Instructions
        context.drawCenteredString(
            font,
            Component.literal("Drag to reorder • Higher = Priority for highlights"),
            width / 2, 35, 0xFFAAAAAA.toInt()
        )

        // Calculate list position
        val listX = (width - LIST_WIDTH) / 2
        val listY = LIST_START_Y

        // Update hovered index
        hoveredIndex = -1
        if (draggedIndex == -1) {
            for (i in priorities.indices) {
                val itemY = listY + i * (ITEM_HEIGHT + ITEM_SPACING)
                if (mouseX >= listX && mouseX <= listX + LIST_WIDTH &&
                    mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT
                ) {
                    hoveredIndex = i
                    break
                }
            }
        }

        // Render priority items
        for (i in priorities.indices) {
            if (i == draggedIndex) continue // Skip the dragged item for now

            val itemY = listY + i * (ITEM_HEIGHT + ITEM_SPACING)
            renderPriorityItem(context, priorities[i], listX, itemY, i)
        }

        // Render dragged item on top
        if (draggedIndex in priorities.indices) {
            val dragY = (LIST_START_Y + draggedIndex * (ITEM_HEIGHT + ITEM_SPACING) + (currentDragY - dragStartY)).toInt()
            renderPriorityItem(context, priorities[draggedIndex], listX, dragY, draggedIndex)
        }

        super.render(context, mouseX, mouseY, delta)
    }

    private fun renderPriorityItem(context: GuiGraphics, priority: MatchPriority, x: Int, y: Int, index: Int) {
        val isHovered = index == hoveredIndex || index == draggedIndex
        val isDragged = index == draggedIndex

        // Background
        val bgColor = when {
            isDragged -> 0x88444444
            isHovered -> 0x66333333
            else -> 0x44222222
        }.toInt()
        context.fill(x, y, x + LIST_WIDTH, y + ITEM_HEIGHT, bgColor)

        // Border
        val borderColor = when {
            isDragged -> 0xFFFFFFFF.toInt()
            isHovered -> 0xFF888888.toInt()
            else -> 0xFF444444.toInt()
        }
        context.renderOutline(x, y, LIST_WIDTH, ITEM_HEIGHT, borderColor)

        // Priority number
        val priorityNum = "#${index + 1}"
        context.drawString(font, priorityNum, x + 8, y + 6, 0xFFFFAA00.toInt())

        // Display name
        context.drawString(font, priority.displayName, x + 40, y + 6, 0xFFFFFFFF.toInt())

        // Description
        context.drawString(font, priority.description, x + 40, y + 17, 0xFF888888.toInt())

        // Drag handle (three lines)
        if (isHovered) {
            val handleX = x + LIST_WIDTH - 20
            val handleY = y + 10
            repeat(3) { i ->
                context.fill(handleX, handleY + i * 3, handleX + 12, handleY + i * 3 + 1, 0xFF888888.toInt())
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) { // Left click
            val listX = (width - LIST_WIDTH) / 2

            for (i in priorities.indices) {
                val itemY = LIST_START_Y + i * (ITEM_HEIGHT + ITEM_SPACING)
                if (mouseX >= listX && mouseX <= listX + LIST_WIDTH &&
                    mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT
                ) {
                    draggedIndex = i
                    dragStartY = mouseY
                    currentDragY = mouseY
                    return true
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && draggedIndex >= 0) {
            // Calculate drop position
            val listY = LIST_START_Y
            val itemTotalHeight = ITEM_HEIGHT + ITEM_SPACING
            val draggedItemY = (listY + draggedIndex * itemTotalHeight + (currentDragY - dragStartY)).toInt()
            val draggedItemCenter = draggedItemY + ITEM_HEIGHT / 2

            // Find new index based on center position
            val newIndex = max(0, min(priorities.size - 1,
                (draggedItemCenter - listY + itemTotalHeight / 2) / itemTotalHeight))

            // Reorder the list
            if (newIndex != draggedIndex) {
                val item = priorities.removeAt(draggedIndex)
                priorities.add(newIndex, item)
            }

            draggedIndex = -1
            return true
        }

        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (button == 0 && draggedIndex >= 0) {
            currentDragY = mouseY
            return true
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
}
