package com.example.myapplicationkoG.editor

import com.example.myapplicationkoG.domain.model.ClothingDocument

/**
 * Snapshot-based undo/redo.
 *
 * Each "commit" pushes the current document onto the undo stack. Undo pops
 * and pushes the prior version onto the redo stack. Any new commit clears
 * the redo stack (standard editor behaviour).
 *
 * Memory: a snapshot is just the document JSON in memory. A typical project
 * with 6 layers, a few brush strokes and a cut path is well under 10 KB.
 * The 50-snapshot cap keeps total memory under ~500 KB even in the worst
 * case.
 */
class UndoRedoManager(private val maxSnapshots: Int = 50) {

    private val undoStack: ArrayDeque<ClothingDocument> = ArrayDeque()
    private val redoStack: ArrayDeque<ClothingDocument> = ArrayDeque()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * Record the current document as a snapshot. Call this AFTER a mutation
     * has been applied to [current] so undo can roll back to it.
     *
     * If [current] equals the most recent snapshot, this is a no-op (avoids
     * stacking no-op undos from idle events).
     */
    fun commit(current: ClothingDocument) {
        if (undoStack.lastOrNull() == current) return
        undoStack.addLast(current)
        while (undoStack.size > maxSnapshots) undoStack.removeFirst()
        redoStack.clear()
    }

    /**
     * Roll back one step. Returns the document to apply, or null if there is
     * nothing to undo.
     */
    fun undo(current: ClothingDocument): ClothingDocument? {
        val target = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return target
    }

    /**
     * Roll forward one step. Returns the document to apply, or null if there
     * is nothing to redo.
     */
    fun redo(current: ClothingDocument): ClothingDocument? {
        val target = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return target
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
