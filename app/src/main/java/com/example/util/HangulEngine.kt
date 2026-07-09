package com.example.util

class HangulEngine {
    private var text = ""
    
    // States for the active composing syllable
    private var cho: Char? = null
    private var jung: Char? = null
    private var jung2: Char? = null
    private var jong: Char? = null
    private var jong2: Char? = null

    companion object {
        val CHO_LIST = listOf(
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
        )
        val JUNG_LIST = listOf(
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 
            'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
        )
        val JONG_LIST = listOf(
            ' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 
            'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
        )

        private val CONSONANTS = setOf(
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
        )

        private val VOWELS = setOf(
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅛ', 
            'ㅜ', 'ㅠ', 'ㅡ', 'ㅣ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅢ'
        )

        fun isConsonant(c: Char): Boolean = CONSONANTS.contains(c)
        fun isVowel(c: Char): Boolean = VOWELS.contains(c)

        fun mergeVowels(v1: Char, v2: Char): Char? {
            return when {
                v1 == 'ㅗ' && v2 == 'ㅏ' -> 'ㅘ'
                v1 == 'ㅗ' && v2 == 'ㅐ' -> 'ㅙ'
                v1 == 'ㅗ' && v2 == 'ㅣ' -> 'ㅚ'
                v1 == 'ㅜ' && v2 == 'ㅓ' -> 'ㅝ'
                v1 == 'ㅜ' && v2 == 'ㅔ' -> 'ㅞ'
                v1 == 'ㅜ' && v2 == 'ㅣ' -> 'ㅟ'
                v1 == 'ㅡ' && v2 == 'ㅣ' -> 'ㅢ'
                else -> null
            }
        }

        fun mergeJongs(j1: Char, j2: Char): Char? {
            return when {
                j1 == 'ㄱ' && j2 == 'ㅅ' -> 'ㄳ'
                j1 == 'ㄴ' && j2 == 'ㅈ' -> 'ㄵ'
                j1 == 'ㄴ' && j2 == 'ㅎ' -> 'ㄶ'
                j1 == 'ㄹ' && j2 == 'ㄱ' -> 'ㄺ'
                j1 == 'ㄹ' && j2 == 'ㅁ' -> 'ㄻ'
                j1 == 'ㄹ' && j2 == 'ㅂ' -> 'ㄼ'
                j1 == 'ㄹ' && j2 == 'ㅅ' -> 'ㄽ'
                j1 == 'ㄹ' && j2 == 'ㅌ' -> 'ㄾ'
                j1 == 'ㄹ' && j2 == 'ㅍ' -> 'ㄿ'
                j1 == 'ㄹ' && j2 == 'ㅎ' -> 'ㅀ'
                j1 == 'ㅂ' && j2 == 'ㅅ' -> 'ㅄ'
                else -> null
            }
        }

        // Decompose compound double jungs (e.g. ㅘ -> ㅗ, ㅏ)
        fun splitJung(v: Char): Pair<Char, Char>? {
            return when (v) {
                'ㅘ' -> Pair('ㅗ', 'ㅏ')
                'ㅙ' -> Pair('ㅗ', 'ㅐ')
                'ㅚ' -> Pair('ㅗ', 'ㅣ')
                'ㅝ' -> Pair('ㅜ', 'ㅓ')
                'ㅞ' -> Pair('ㅜ', 'ㅔ')
                'ㅟ' -> Pair('ㅜ', 'ㅣ')
                'ㅢ' -> Pair('ㅡ', 'ㅣ')
                else -> null
            }
        }

        // Decompose compound double jongs (e.g. ㄳ -> ㄱ, ㅅ)
        fun splitJong(j: Char): Pair<Char, Char>? {
            return when (j) {
                'ㄳ' -> Pair('ㄱ', 'ㅅ')
                'ㄵ' -> Pair('ㄴ', 'ㅈ')
                'ㄶ' -> Pair('ㄴ', 'ㅎ')
                'ㄺ' -> Pair('ㄹ', 'ㄱ')
                'ㄻ' -> Pair('ㄹ', 'ㅁ')
                'ㄼ' -> Pair('ㄹ', 'ㅂ')
                'ㄽ' -> Pair('ㄹ', 'ㅅ')
                'ㄾ' -> Pair('ㄹ', 'ㅌ')
                'ㄿ' -> Pair('ㄹ', 'ㅍ')
                'ㅀ' -> Pair('ㄹ', 'ㅎ')
                'ㅄ' -> Pair('ㅂ', 'ㅅ')
                else -> null
            }
        }

        val QWERTY_MAP = mapOf(
            'q' to 'ㅂ', 'w' to 'ㅈ', 'e' to 'ㄷ', 'r' to 'ㄱ', 't' to 'ㅅ',
            'y' to 'ㅛ', 'u' to 'ㅕ', 'i' to 'ㅑ', 'o' to 'ㅐ', 'p' to 'ㅔ',
            'a' to 'ㅁ', 's' to 'ㄴ', 'd' to 'ㅇ', 'f' to 'ㄹ', 'g' to 'ㅎ',
            'h' to 'ㅗ', 'j' to 'ㅓ', 'k' to 'ㅏ', 'l' to 'ㅣ',
            'z' to 'ㅋ', 'x' to 'ㅌ', 'c' to 'ㅊ', 'v' to 'ㅍ', 'b' to 'ㅠ',
            'n' to 'ㅜ', 'm' to 'ㅡ',
            'Q' to 'ㅃ', 'W' to 'ㅉ', 'E' to 'ㄸ', 'R' to 'ㄲ', 'T' to 'ㅆ',
            'Y' to 'ㅛ', 'U' to 'ㅕ', 'I' to 'ㅑ', 'O' to 'ㅒ', 'P' to 'ㅖ',
            'A' to 'ㅁ', 'S' to 'ㄴ', 'D' to 'ㅇ', 'F' to 'ㄹ', 'G' to 'ㅎ',
            'H' to 'ㅗ', 'J' to 'ㅓ', 'K' to 'ㅏ', 'L' to 'ㅣ',
            'Z' to 'ㅋ', 'X' to 'ㅌ', 'C' to 'ㅊ', 'V' to 'ㅍ', 'B' to 'ㅠ',
            'N' to 'ㅜ', 'M' to 'ㅡ'
        )
    }

    fun processTextInput(oldValue: String, newValue: String, isKoreanMode: Boolean): String {
        if (!isKoreanMode) {
            clear()
            setInitialText(newValue)
            return newValue
        }

        if (newValue.length < oldValue.length) {
            val diff = oldValue.length - newValue.length
            repeat(diff) {
                handleBackspace()
            }
            return getAssembledText()
        } else if (newValue.length > oldValue.length) {
            val addedText = newValue.substring(oldValue.length)
            for (char in addedText) {
                val mappedChar = QWERTY_MAP[char] ?: char
                inputKey(mappedChar)
            }
            return getAssembledText()
        }
        return newValue
    }

    fun setInitialText(initial: String) {
        text = initial
        clearComposition()
    }

    fun getAssembledText(): String {
        return text + getComposingSyllable()
    }

    private fun getComposingSyllable(): String {
        val activeCho = cho ?: return ""
        val activeJung = jung ?: return activeCho.toString()
        
        val finalJung = if (jung2 != null) {
            mergeVowels(activeJung, jung2!!) ?: activeJung
        } else {
            activeJung
        }

        val activeJong = jong ?: return makeSyllable(activeCho, finalJung, ' ')

        val finalJong = if (jong2 != null) {
            mergeJongs(activeJong, jong2!!) ?: activeJong
        } else {
            activeJong
        }

        return makeSyllable(activeCho, finalJung, finalJong)
    }

    private fun makeSyllable(c: Char, v: Char, j: Char): String {
        val choIdx = CHO_LIST.indexOf(c)
        val jungIdx = JUNG_LIST.indexOf(v)
        val jongIdx = JONG_LIST.indexOf(j)

        if (choIdx == -1 || jungIdx == -1) {
            return "" + c + (if (v != ' ') v else "")
        }

        val code = ((choIdx * 21) + jungIdx) * 28 + (if (jongIdx == -1) 0 else jongIdx) + 0xAC00
        return code.toChar().toString()
    }

    fun inputKey(c: Char) {
        if (!isConsonant(c) && !isVowel(c)) {
            // Standalone punctuation, spaces or english characters: finalize previous and append
            text = getAssembledText() + c
            clearComposition()
            return
        }

        if (isConsonant(c)) {
            handleConsonant(c)
        } else {
            handleVowel(c)
        }
    }

    private fun handleConsonant(c: Char) {
        if (cho == null) {
            // State: EMPTY -> CHO
            cho = c
        } else if (jung == null) {
            // State: CHO -> CHO. (Previous consonant is finalized, new consonant starts a syllable)
            text += cho
            cho = c
        } else if (jong == null) {
            // State: CHO_JUNG -> CHO_JUNG_JONG
            if (JONG_LIST.contains(c)) {
                jong = c
            } else {
                // If it can't be a final consonant (like ㄸ, ㅃ, ㅉ), finalize current syllable and start new
                text = getAssembledText()
                clearComposition()
                cho = c
            }
        } else if (jong2 == null) {
            // State: CHO_JUNG_JONG -> CHO_JUNG_JONG with double jong
            val merged = mergeJongs(jong!!, c)
            if (merged != null) {
                jong2 = c
            } else {
                // If they can't be merged, finalize current syllable and start new with the consonant
                text = getAssembledText()
                clearComposition()
                cho = c
            }
        } else {
            // Already has double jong, finalize and start new
            text = getAssembledText()
            clearComposition()
            cho = c
        }
    }

    private fun handleVowel(c: Char) {
        if (cho == null) {
            // No initial consonant, just append standalone vowel to text directly
            text += c
        } else if (jung == null) {
            // State: CHO -> CHO_JUNG
            jung = c
        } else if (jong == null) {
            // State: CHO_JUNG -> CHO_JUNG (check for compound vowel)
            val merged = mergeVowels(jung!!, c)
            if (merged != null) {
                jung2 = c
            } else {
                // Can't merge, finalize previous and start standalone vowel
                text = getAssembledText()
                clearComposition()
                text += c
            }
        } else if (jong2 != null) {
            // State: CHO_JUNG_JONG (double jong) -> Vowel migration
            // The second jong (jong2) becomes the initial consonant of the new syllable
            val prevJong = jong
            val nextCho = jong2
            
            // Finalize first syllable with only first jong
            val finalJung = if (jung2 != null) mergeVowels(jung!!, jung2!!) ?: jung!! else jung!!
            text += makeSyllable(cho!!, finalJung, prevJong!!)

            // Start new syllable with nextCho & current vowel
            clearComposition()
            cho = nextCho
            jung = c
        } else {
            // State: CHO_JUNG_JONG (single jong) -> Vowel migration
            // The single jong becomes the initial consonant of the new syllable
            val nextCho = jong
            
            // Finalize first syllable with NO jong
            val finalJung = if (jung2 != null) mergeVowels(jung!!, jung2!!) ?: jung!! else jung!!
            text += makeSyllable(cho!!, finalJung, ' ')

            // Start new syllable with nextCho & current vowel
            clearComposition()
            cho = nextCho
            jung = c
        }
    }

    fun handleBackspace(): Boolean {
        // Returns true if anything was deleted, false if completely empty
        if (jong2 != null) {
            jong2 = null
            return true
        }
        if (jong != null) {
            jong = null
            return true
        }
        if (jung2 != null) {
            jung2 = null
            return true
        }
        if (jung != null) {
            jung = null
            return true
        }
        if (cho != null) {
            cho = null
            return true
        }

        // If no active composition, delete the last char of the finalized text
        if (text.isNotEmpty()) {
            // Check if the last character of finalized text can be decomposed!
            val lastChar = text.last()
            if (lastChar.code in 0xAC00..0xD7A3) {
                // Decompose the last Hangul syllable to let the user edit it!
                text = text.substring(0, text.length - 1)
                decomposeSyllable(lastChar)
            } else {
                text = text.substring(0, text.length - 1)
            }
            return true
        }
        return false
    }

    private fun decomposeSyllable(char: Char) {
        val totalCode = char.code - 0xAC00
        val jongIdx = totalCode % 28
        val totalJungIdx = totalCode / 28
        val jungIdx = totalJungIdx % 21
        val choIdx = totalJungIdx / 21

        cho = CHO_LIST.getOrNull(choIdx)
        
        val fullJung = JUNG_LIST.getOrNull(jungIdx)
        if (fullJung != null) {
            val splitJ = splitJung(fullJung)
            if (splitJ != null) {
                jung = splitJ.first
                jung2 = splitJ.second
            } else {
                jung = fullJung
            }
        }

        val fullJong = JONG_LIST.getOrNull(jongIdx)
        if (fullJong != null && fullJong != ' ') {
            val splitJo = splitJong(fullJong)
            if (splitJo != null) {
                jong = splitJo.first
                jong2 = splitJo.second
            } else {
                jong = fullJong
            }
        }
    }

    fun clear() {
        text = ""
        clearComposition()
    }

    private fun clearComposition() {
        cho = null
        jung = null
        jung2 = null
        jong = null
        jong2 = null
    }
}
