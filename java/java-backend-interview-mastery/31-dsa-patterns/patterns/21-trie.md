# Pattern 21 — Trie (Prefix Tree)

## Identify it when
- The problem is about **words sharing prefixes**: autocomplete, dictionary lookup, "startsWith", prefix counting, wildcard word search, word-break with a dictionary.
- You'd otherwise do many O(L) hash lookups and want **prefix queries** (not just exact match) efficiently.
- "Implement a trie", "word dictionary with `.` wildcard", "longest word in dictionary", "replace words with roots", "maximum XOR of two numbers" (bitwise trie).

**Core idea:** a tree where each edge is a character; a path from the root spells a prefix; a flag marks the end of a complete word. Insert/search/startsWith are **O(L)** in the word length, independent of how many words are stored.

## Template
```java
class TrieNode {
    TrieNode[] children = new TrieNode[26];   // or a HashMap<Character,TrieNode> for large alphabets
    boolean isWord;
}
```

## Problems

### P1. Implement Trie (insert / search / startsWith)
```java
class Trie {
    private final TrieNode root = new TrieNode();
    void insert(String w) {
        TrieNode n = root;
        for (char c : w.toCharArray()) {
            int i = c - 'a';
            if (n.children[i] == null) n.children[i] = new TrieNode();
            n = n.children[i];
        }
        n.isWord = true;                        // mark end of a complete word
    }
    boolean search(String w) { TrieNode n = find(w); return n != null && n.isWord; }
    boolean startsWith(String p) { return find(p) != null; }
    private TrieNode find(String s) {
        TrieNode n = root;
        for (char c : s.toCharArray()) {
            n = n.children[c - 'a'];
            if (n == null) return null;
        }
        return n;
    }
}
```
**Why:** `search` requires the end-of-word flag; `startsWith` only needs the path to exist. **Each op O(L), Space O(total chars · alphabet).**

### P2. Word dictionary with `.` wildcard (DFS over the trie)
```java
class WordDictionary {
    private final TrieNode root = new TrieNode();
    void addWord(String w) { /* same as Trie.insert */ }
    boolean search(String w) { return dfs(w, 0, root); }
    private boolean dfs(String w, int k, TrieNode node) {
        if (node == null) return false;
        if (k == w.length()) return node.isWord;
        char c = w.charAt(k);
        if (c == '.') {                          // wildcard: try every child
            for (TrieNode child : node.children)
                if (child != null && dfs(w, k + 1, child)) return true;
            return false;
        }
        return dfs(w, k + 1, node.children[c - 'a']);  // normal char
    }
}
```
**Why:** `.` branches into all children — a DFS over the trie. **Time O(L) normal, O(26^(#dots)) worst, Space O(L)** recursion.

### P3. Replace words with their shortest root (prefix match)
```java
String replaceWords(List<String> roots, String sentence) {
    Trie trie = new Trie();
    for (String r : roots) trie.insert(r);
    StringBuilder sb = new StringBuilder();
    for (String word : sentence.split(" ")) {
        if (sb.length() > 0) sb.append(' ');
        sb.append(trie.shortestРrefixRoot(word));   // walk until the first isWord node
    }
    return sb.toString();
}
// (shortestPrefixRoot walks the trie char by char, returning the prefix at the first isWord=true node,
//  or the whole word if no root matches.)
```
**Why:** the trie finds the shortest matching root in O(word length). **Time O(total chars), Space O(dictionary chars).**

### P4. Word search II (trie + grid backtracking — classic hard)
```java
// Build a trie of all target words; DFS the grid, walking the trie in lockstep so you
// prune the moment the current path isn't a prefix of ANY word. Mark trie nodes with the
// full word at their end; collect when you hit isWord. This is far faster than searching
// each word separately because the trie shares prefixes across the whole dictionary.
// Time ~O(R·C·4^maxLen) worst but heavily pruned; Space O(total chars).
```
**Why:** the trie prunes dead grid paths across *all* words at once — the reason to combine trie with backtracking (patterns 21 + 17).

## Follow-ups & optimizations
- *Array vs HashMap children:* a 26-slot array is fast and simple for lowercase letters; a `HashMap<Character,TrieNode>` saves memory for sparse/large alphabets (Unicode).
- *"Memory is huge for many short words?"* → a trie shares prefixes, so it's often *more* compact than storing full strings; for extreme cases consider a compressed/radix trie.
- *"Count words with a prefix?"* → store a counter at each node incremented on insert.
- *Bitwise trie:* "maximum XOR of two numbers" builds a trie of the numbers' bits and greedily walks the opposite bit at each level — a neat non-string trie application.
- *Delete:* trickier — unmark `isWord`, and prune nodes with no children and not ending another word.

## Complexity summary
insert / search / startsWith: **O(L) time** (L = word/prefix length), **independent of the number of stored words** — the key advantage over a set of strings for prefix queries. Space is **O(total characters · alphabet factor)**. Trie shines whenever **prefixes** matter; for exact-match only, a HashSet is simpler.
