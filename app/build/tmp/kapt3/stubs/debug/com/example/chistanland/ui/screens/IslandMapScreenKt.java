package com.example.chistanland.ui.screens;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000J\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\u001a2\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0007\u001a&\u0010\t\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\f2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0007\u001a[\u0010\u000e\u001a\u00020\u0001\"\u0004\b\u0000\u0010\u000f*\u00020\u00102\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u0002H\u000f0\u00122;\u0010\u0013\u001a7\u0012\u0013\u0012\u00110\u0015\u00a2\u0006\f\b\u0016\u0012\b\b\u0017\u0012\u0004\b\b(\u0018\u0012\u0013\u0012\u0011H\u000f\u00a2\u0006\f\b\u0016\u0012\b\b\u0017\u0012\u0004\b\b(\n\u0012\u0004\u0012\u00020\u00010\u0014\u00a2\u0006\u0002\b\u0019\u00a8\u0006\u001a"}, d2 = {"IslandMapScreen", "", "viewModel", "Lcom/example/chistanland/ui/LearningViewModel;", "onStartItem", "Lkotlin/Function1;", "Lcom/example/chistanland/data/LearningItem;", "onOpenParentPanel", "Lkotlin/Function0;", "IslandNode", "item", "isLocked", "", "onClick", "itemsIndexed", "T", "Landroidx/compose/foundation/lazy/LazyListScope;", "items", "", "itemContent", "Lkotlin/Function2;", "", "Lkotlin/ParameterName;", "name", "index", "Landroidx/compose/runtime/Composable;", "app_debug"})
public final class IslandMapScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void IslandMapScreen(@org.jetbrains.annotations.NotNull()
    com.example.chistanland.ui.LearningViewModel viewModel, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.chistanland.data.LearningItem, kotlin.Unit> onStartItem, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onOpenParentPanel) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void IslandNode(@org.jetbrains.annotations.NotNull()
    com.example.chistanland.data.LearningItem item, boolean isLocked, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    public static final <T extends java.lang.Object>void itemsIndexed(@org.jetbrains.annotations.NotNull()
    androidx.compose.foundation.lazy.LazyListScope $this$itemsIndexed, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends T> items, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.lang.Integer, ? super T, kotlin.Unit> itemContent) {
    }
}