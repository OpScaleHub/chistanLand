package com.example.chistanland.ui.screens;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000@\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0000\u001a\u0010\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0007\u001a&\u0010\u0004\u001a\u00020\u00012\u0006\u0010\u0005\u001a\u00020\u00062\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\u0006\u0010\t\u001a\u00020\nH\u0007\u001a:\u0010\u000b\u001a\u00020\u00012\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00060\r2\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u000f2\u0006\u0010\u0010\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\nH\u0007\u001a\u001e\u0010\u0012\u001a\u00020\u00012\u0006\u0010\u0013\u001a\u00020\u00142\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0007\u001a\u0010\u0010\u0016\u001a\u00020\u00012\u0006\u0010\u0017\u001a\u00020\u0003H\u0007\u001a\u0010\u0010\u0018\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0007\u001a0\u0010\u0019\u001a\u00020\u00012\u0006\u0010\u001a\u001a\u00020\u00062\u0006\u0010\u001b\u001a\u00020\u00062\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\n0\r2\b\b\u0002\u0010\u001d\u001a\u00020\u001eH\u0007\u00a8\u0006\u001f"}, d2 = {"ChickStatus", "", "streak", "", "KeyButton", "char", "", "onClick", "Lkotlin/Function0;", "isHighlighted", "", "KidKeyboard", "keys", "", "onKeyClick", "Lkotlin/Function1;", "targetChar", "showHint", "LearningSessionScreen", "viewModel", "Lcom/example/chistanland/ui/LearningViewModel;", "onBack", "PlantProgress", "level", "StreakIndicator", "WordDisplay", "targetWord", "typedText", "charStatus", "modifier", "Landroidx/compose/ui/Modifier;", "app_debug"})
public final class LearningSessionScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void LearningSessionScreen(@org.jetbrains.annotations.NotNull()
    com.example.chistanland.ui.LearningViewModel viewModel, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void StreakIndicator(int streak) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void ChickStatus(int streak) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void PlantProgress(int level) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void WordDisplay(@org.jetbrains.annotations.NotNull()
    java.lang.String targetWord, @org.jetbrains.annotations.NotNull()
    java.lang.String typedText, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Boolean> charStatus, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void KidKeyboard(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> keys, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onKeyClick, @org.jetbrains.annotations.NotNull()
    java.lang.String targetChar, boolean showHint) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void KeyButton(@org.jetbrains.annotations.NotNull()
    java.lang.String p0_1526187, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick, boolean isHighlighted) {
    }
}