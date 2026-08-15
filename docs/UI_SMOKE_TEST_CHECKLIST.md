# Genie UI Smoke-Test Checklist

**Scope**: GenieDialogScreen, RadialGenieMenu, PocketConfirmScreen, OwnerDeathChoiceScreen  
**Environments**: TLauncher (client) + Dedicated Server  
**GUI scale**: 3 (default) and 4

## 1. GenieDialogScreen
- [ ] Open via right-click on owned genie (empty hand)
- [ ] Type text → press Enter or click Send → response appears with correct color (emotionLevel)
- [ ] Click "Orders" → radial menu opens
- [ ] Hover/keyboard (arrows/WASD) → gold highlight + tooltip appears
- [ ] Select mode via mouse or keyboard → mode changes, menu closes, command sent
- [ ] Escape while menu open → only menu closes
- [ ] Escape while menu closed → screen closes
- [ ] Dangerous wish (META_RULE) → Confirm/Cancel buttons appear below response
- [ ] Waiting state: input dimmed + gray border while request in flight
- [ ] Policy buttons positioned below response text, above input field

## 2. RadialGenieMenu
- [ ] 4 sectors visible with correct labels
- [ ] Mouse hover → gold background + tooltip
- [ ] Keyboard navigation (↑↓←→ / WASD) → gold background + focus ring
- [ ] Click outside menu or Esc → closes cleanly
- [ ] No overlap with other UI elements

## 3. PocketConfirmScreen
- [ ] Trigger via "карманная сцена" or `/genie scene`
- [ ] Risk text color matches level (green/yellow/red)
- [ ] Risk description sentence appears below risk line
- [ ] Confirm → sends C2SPocketConfirm, screen closes
- [ ] Rollback → sends C2SPocketRollback, screen closes
- [ ] Escape → closes without sending either packet (silent dismiss)
- [ ] Narration labels readable by screen reader

## 4. OwnerDeathChoiceScreen
- [ ] Appears after owner death (pause screen)
- [ ] 4 buttons with consequence descriptions below each
- [ ] Each choice sends correct DeathChoice packet
- [ ] Escape → closes without choice (owner/genie state unresolved)
- [ ] Color coding or visual distinction between options visible

## 5. Dedicated Server
- [ ] All above flows work when connected to dedicated server
- [ ] No client-only class references from common/server code
- [ ] No crashes or desync on packet handling

## 6. Accessibility & Polish
- [ ] All interactive elements have narration labels
- [ ] Focus ring visible on keyboard navigation
- [ ] No text truncation at GUI scale 3/4
- [ ] Consistent use of KHColors tokens across all screens
- [ ] No overlapping elements or layout issues

**Status**: Ready for TLauncher + dedicated server smoke test.