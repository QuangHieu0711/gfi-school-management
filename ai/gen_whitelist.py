"""Generate the complete set of valid Vietnamese syllables (ASCII form, no diacritics)."""
import itertools

# All Vietnamese onset consonants
ONSETS = [
    '', 'b', 'c', 'ch', 'd', 'g', 'gh', 'gi', 'h', 'k', 'kh',
    'l', 'm', 'n', 'ng', 'ngh', 'nh', 'p', 'ph', 'qu', 'r', 's',
    't', 'th', 'tr', 'v', 'x',
]

# All Vietnamese rimes (nucleus + optional coda)
RIMES = [
    'a', 'ac', 'ach', 'ai', 'am', 'an', 'ang', 'anh', 'ao', 'ap', 'at', 'au', 'ay',
    'e', 'ec', 'em', 'en', 'eng', 'eo', 'ep', 'et',
    'i', 'ia', 'ich', 'iem', 'ien', 'ieng', 'iep', 'iet', 'ieu',
    'im', 'in', 'inh', 'ip', 'it', 'iu',
    'o', 'oa', 'oac', 'oach', 'oai', 'oam', 'oan', 'oang', 'oanh',
    'oap', 'oat', 'oay',
    'oc', 'oe', 'oem', 'oen', 'oet', 'oi', 'om', 'on', 'ong', 'op', 'ot',
    'u', 'ua', 'uai', 'uan', 'uang', 'uat', 'uay',
    'uc', 'ui', 'um', 'un', 'ung', 'uoc', 'uoi', 'uom', 'uon', 'uong', 'uot',
    'up', 'ut', 'uy', 'uya', 'uyen', 'uynh', 'uyt', 'uyu',
    'y', 'yem', 'yen', 'yet', 'yeu',
]

syllables = set()
for onset, rime in itertools.product(ONSETS, RIMES):
    syl = onset + rime
    if len(syl) >= 2:
        syllables.add(syl)

# Also add single-letter Vietnamese words
syllables.update(['a', 'i', 'o', 'u', 'y'])

print(f"Total syllables: {len(syllables)}")

# Test
tests = ['hien', 'quen', 'biet', 'thao', 'bola', 'pointscore', 'exercise', 'homework',
         'ball', 'catch', 'throw', 'run', 'jump', 'good', 'better', 'student',
         'nam', 'dong', 'tac', 'phoi', 'hop', 'ngang', 'dan', 'hang']
for w in tests:
    status = "VIET" if w in syllables else "FOREIGN"
    print(f"  {w}: {status}")

# Write the set as Python code
with open('viet_syllables_set.py', 'w', encoding='utf-8') as f:
    f.write("# Auto-generated: all valid Vietnamese syllables in ASCII (no diacritics)\n")
    f.write(f"# Total: {len(syllables)} syllables\n")
    f.write("VIET_SYLLABLES = {\n")
    sorted_syls = sorted(syllables)
    line = "    "
    for i, s in enumerate(sorted_syls):
        chunk = f'"{s}", '
        if len(line) + len(chunk) > 100:
            f.write(line.rstrip() + "\n")
            line = "    "
        line += chunk
    if line.strip():
        f.write(line.rstrip().rstrip(',') + "\n")
    f.write("}\n")

print(f"\nWritten to viet_syllables_set.py")
