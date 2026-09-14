from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def test_character_sheet_uses_lazy_two_column_sections():
    source = read("app/src/main/java/com/dubl/character/android/ui/screens/OverviewScreen.kt")
    assert "ownedSkillSectionItems(" in source
    assert "ownedDevelopmentSectionItems(" in source
    assert source.count("compactGridRows(") >= 2
    assert 'columns = 2' in source


def test_group_manager_has_explicit_drag_drop_targeting():
    source = read("app/src/main/java/com/dubl/character/android/ui/screens/OverviewScreen.kt")
    assert "GroupManagerItemCard(" in source
    assert "onGloballyPositioned" in source
    assert "hoveredGroupId" in source
    assert "SheetGroupingRules.moveItem(" in source
    assert "≡" in source


def test_bottom_sheet_releases_downward_overscroll():
    policy = read("app/src/main/java/com/dubl/character/android/ui/components/SheetInteractionPolicy.kt")
    assert "fromUserInput && availableY < 0f" in policy


def test_keyboard_focus_is_cleared_from_app_and_sheets():
    app = read("app/src/main/java/com/dubl/character/android/ui/DublApp.kt")
    sheets = read("app/src/main/java/com/dubl/character/android/ui/components/SheetInteraction.kt")
    assert "Modifier.dismissKeyboardOnPointerDown()" in app
    assert ".dismissKeyboardOnPointerDown()" in sheets


def test_martial_art_availability_is_precomputed_for_scroll():
    source = read("app/src/main/java/com/dubl/character/android/ui/screens/FeatsScreen.kt")
    assert "martialAvailabilityById" in source
    assert "filteredEntries.associate { entry -> entry.id to rules.availability(entry) }" in source
    assert "availabilityOverride = martialAvailabilityById[entry.id]" in source
