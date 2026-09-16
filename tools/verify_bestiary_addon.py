from pathlib import Path
import json,re,sys
root=Path(__file__).resolve().parents[1]
checks=[]
def check(name, cond): checks.append((name,bool(cond)))
def text(rel): return (root/rel).read_text(encoding='utf-8')
java='\n'.join(p.read_text(encoding='utf-8',errors='ignore') for p in (root/'src/main/java').rglob('*.java'))
build=text('build.gradle'); mods=text('src/main/resources/META-INF/mods.toml')
network=text('src/main/java/com/szypxj/tlcreaturebestiary/network/BestiaryNetwork.java')
search=text('src/main/java/com/szypxj/tlcreaturebestiary/client/BestiarySearchPolicy.java')
screen=text('src/main/java/com/szypxj/tlcreaturebestiary/client/screen/BestiaryScreen.java')
star=text('src/main/java/com/szypxj/tlcreaturebestiary/client/DangerStarRenderer.java')
spy=text('src/main/java/com/szypxj/tlcreaturebestiary/client/BestiarySpyglassTitleExtension.java')
check('addon has no Mixin dependency/config', 'mixin' not in build.lower() and not list((root/'src/main/resources').glob('*mixins*.json')) and '@Mixin' not in java)
check('mods.toml hard-depends on TDMC', 'modId="tl_domesticate_more_creatures"' in mods and 'mandatory=true' in mods)
check('addon protocol fixed at 1', re.search(r'PROTOCOL\s*=\s*"1"',network) is not None)
check('network IDs explicit 0..3', all(f'= {i};' in network for i in range(4)))
check('DangerRating owns sqrt threshold algorithm', 'Math.sqrt' in text('src/main/java/com/szypxj/tlcreaturebestiary/danger/DangerRating.java'))
check('same DangerStarRenderer used by spyglass and bestiary screen', 'DangerStarRenderer' in spy and 'DangerStarRenderer' in screen)
index=text('src/main/java/com/szypxj/tlcreaturebestiary/data/BestiaryEntryIndex.java')
check('discovered non-default-attribute types are merged into index', 'ClientBestiaryState.unlocked()' in screen and 'discovered' in index and 'result.addAll(discovered)' in index)
locked_before_names = search.find('if (!unlocked')
name_use = min([p for p in (search.find('String name ='), search.find('String registryId =')) if p>=0], default=-1)
check('locked search exits before real-name matching', locked_before_names >=0 and name_use>=0 and locked_before_names < name_use)
check('no forbidden section gray colors', '§7' not in java and '§8' not in java and '§7' not in '\n'.join(p.read_text(encoding='utf-8') for p in (root/'src/main/resources/assets/tl_creature_bestiary/lang').glob('*.json')) and '§8' not in '\n'.join(p.read_text(encoding='utf-8') for p in (root/'src/main/resources/assets/tl_creature_bestiary/lang').glob('*.json')))
check('no world-wide entity scan APIs', not any(x in java for x in ('getEntitiesOfClass(','getAllEntities(','getEntities(')))
check('S2C packet handlers are client-dist guarded', all('DistExecutor.unsafeRunWhenOn' in text(rel) for rel in (
    'src/main/java/com/szypxj/tlcreaturebestiary/network/packet/S2CBestiaryFullSync.java',
    'src/main/java/com/szypxj/tlcreaturebestiary/network/packet/S2CBestiaryUnlock.java',
    'src/main/java/com/szypxj/tlcreaturebestiary/network/packet/S2CBestiaryDetail.java')))
# Strict visible-text rule for known current exceptions.
check('star glyph comes from I18N, not Java literal', 'private static final String STAR = "★"' not in star and 'Component.translatable("gui.tl_creature_bestiary.star")' in star)
check('missing item label uses I18N, not registry-id literal', 'Component.literal(food.itemId().toString())' not in screen and 'gui.tl_creature_bestiary.unknown_item' in screen)
check('unconfigured native taming food has explicit I18N state', 'gui.tl_creature_bestiary.food_unconfigured' in screen and 'food.configured()' in screen)
# Extract static translatable keys and verify both language files.
zh=json.loads(text('src/main/resources/assets/tl_creature_bestiary/lang/zh_cn.json'))
en=json.loads(text('src/main/resources/assets/tl_creature_bestiary/lang/en_us.json'))
keys=set(re.findall(r'Component\.translatable\("([^"]+)"', java))
missing_zh=sorted(k for k in keys if k.startswith(('gui.tl_creature_bestiary.','msg.tl_creature_bestiary.')) and k not in zh)
missing_en=sorted(k for k in keys if k.startswith(('gui.tl_creature_bestiary.','msg.tl_creature_bestiary.')) and k not in en)
check('all addon translatable keys exist in zh_cn', not missing_zh)
check('all addon translatable keys exist in en_us', not missing_en)
if missing_zh: print('MISSING_ZH='+','.join(missing_zh))
if missing_en: print('MISSING_EN='+','.join(missing_en))
failed=[n for n,v in checks if not v]
for n,v in checks: print(('PASS' if v else 'FAIL')+': '+n)
print(f'CHECKS={len(checks)} FAILURES={len(failed)}')
sys.exit(1 if failed else 0)
