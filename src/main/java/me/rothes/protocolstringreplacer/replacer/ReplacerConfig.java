package me.rothes.protocolstringreplacer.replacer;

import me.rothes.protocolstringreplacer.ProtocolStringReplacer;
import me.rothes.protocolstringreplacer.api.configuration.CommentYamlConfiguration;
import me.rothes.protocolstringreplacer.api.configuration.DotYamlConfiguration;
import org.apache.commons.collections.map.ListOrderedMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplacerConfig {

    private File file;
    private DotYamlConfiguration configuration;
    private boolean enable;
    private int priority;
    private List<ListenType> listenTypeList = new ArrayList<>();
    private MatchType matchType;
    private HashMap<ReplacesType, ListOrderedMap> replaces = new HashMap<>();
    private HashMap<ReplacesType, HashMap<Short, LinkedList<CommentLine>>> commentLines = new HashMap<>();
    private String author;
    private String version;
    private boolean edited;

    public static class CommentLine {
        private String key;
        private String value;

        private CommentLine(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    public enum MatchType {
        CONTAIN("Contain"),
        EQUAL("Equal"),
        REGEX("Regex");

        private String name;

        MatchType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public ReplacerConfig(@Nonnull File file, @Nonnull DotYamlConfiguration configuration) {
        long startTime = System.nanoTime();
        loadData(file, configuration);
        if (ProtocolStringReplacer.getInstance().getConfig().getBoolean("Options.Features.Console.Print-Replacer-Config-When-Loaded", false)) {
            Bukkit.getConsoleSender().sendMessage("§7[§cProtocol§6StringReplacer§7] §a载入替换配置: " + getRelativePath() + ". §8耗时 " + (System.nanoTime() - startTime) / 1000000d + "ms");
        }
    }

    public boolean isEdited() {
        return edited;
    }

    public File getFile() {
        return file;
    }

    public boolean isEnable() {
        return enable;
    }

    public DotYamlConfiguration getConfiguration() {
        return configuration;
    }

    public int getPriority() {
        return priority;
    }

    public List<ListenType> getListenTypeList() {
        return listenTypeList;
    }

    @Nullable
    public ListOrderedMap getReplaces(ReplacesType replacesType) {
        return replaces.get(replacesType);
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public String getRelativePath() {
        return file.getAbsolutePath().substring((ProtocolStringReplacer.getInstance().getDataFolder().getAbsolutePath() + "\\").length()).replace('\\', '/');
    }

    public HashMap<Short, LinkedList<CommentLine>> getCommentLines(ReplacesType replacesType) {
        return commentLines.get(replacesType);
    }

    public void saveConfig() {
        configuration.set("Options鰠Enable", enable);
        configuration.set("Options鰠Priority", priority);
        configuration.set("Options鰠Author", author);
        configuration.set("Options鰠Version", version);
        LinkedList<String> types = new LinkedList<>();
        for (var listenType : listenTypeList) {
            types.add(listenType.getName());
        }
        configuration.set("Options鰠Filter鰠Listen-Types", types);
        configuration.set("Options鰠Match-Type", matchType.getName());
        configuration.set("Replaces鰠Common", new ArrayList<String>());
        HashMap<Short, LinkedList<CommentLine>> commentLines = this.commentLines.get(ReplacesType.COMMON);
        ListOrderedMap replaces = this.replaces.get(ReplacesType.COMMON);
        if (matchType == MatchType.REGEX) {
            for (short i = 0; i < replaces.size(); i++) {
                if (commentLines.containsKey(i)) {
                    LinkedList<CommentLine> commentLineList = commentLines.get(i);
                    for (var commentLine: commentLineList) {
                        configuration.set("Replaces鰠Common鰠" + commentLine.key, commentLine.value);
                    }
                }
                Pattern pattern = (Pattern) replaces.get(i);
                configuration.set("Replaces鰠Common鰠" + pattern.toString(), replaces.get(pattern));
            }
        } else {
            for (short i = 0; i < replaces.size(); i++) {
                if (commentLines.containsKey(i)) {
                    LinkedList<CommentLine> commentLineList = commentLines.get(i);
                    for (var commentLine: commentLineList) {
                        configuration.set("Replaces鰠Common鰠" + commentLine.key, commentLine.value);
                    }
                }
                String string = (String) replaces.get(i);
                configuration.set("Replaces鰠Common鰠" + string, replaces.get(string));
            }
        }
        try {
            configuration.save(file);
            edited = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setReplace(int index, @Nonnull String value, @Nonnull ReplacesType replacesType) {
        if (index < replaces.size()) {
            replaces.get(replacesType).setValue(index, value);
            edited = true;
            saveConfig();
        }
    }

    public void setReplace(int index, @Nonnull String key, @Nonnull String value, @Nonnull ReplacesType replacesType) {
        if (index < replaces.size()) {
            removeReplace(index, false, replacesType);
        }
        if (index <= replaces.size()) {
            addReplace(index, key, value, replacesType);
            edited = true;
            saveConfig();
        }
    }

    public void addReplace(int index, @Nonnull String key, @Nonnull String value, @Nonnull ReplacesType replacesType) {
        if (index <= replaces.size()) {
            if (this.matchType == MatchType.REGEX) {
                replaces.get(replacesType).put(index, Pattern.compile(key), value);
            } else {
            replaces.get(replacesType).put(index, key, value);
            }
            HashMap<Short, LinkedList<CommentLine>> commentLines = new HashMap<>();
            for (Map.Entry<Short, LinkedList<CommentLine>> entry : this.commentLines.get(replacesType).entrySet()) {
                short i = entry.getKey();
                LinkedList<CommentLine> commentLineList = entry.getValue();
                if (i >= index) {
                    commentLines.put((short) (i + 1), commentLineList);
                } else {
                    commentLines.put(i, commentLineList);
                }
                this.commentLines.put(replacesType, commentLines);
            }
            edited = true;
            saveConfig();
        }
    }

    public void addReplace(@Nonnull String key, @Nonnull String value, @Nonnull ReplacesType replacesType) {
        addReplace(replaces.size(), key, value, replacesType);
    }

    public void removeReplace(int index, @Nonnull ReplacesType replacesType) {
        removeReplace(index, true, replacesType);
        edited = true;
        saveConfig();
    }

    public void removeReplace(int index, boolean editComment, @Nonnull ReplacesType replacesType) {
        HashMap<Short, LinkedList<CommentLine>> commentLines = new HashMap<>();
        if (editComment) {
            for (Map.Entry<Short, LinkedList<CommentLine>> entry : this.commentLines.get(replacesType).entrySet()) {
                short i = entry.getKey();
                LinkedList<CommentLine> commentLineList = entry.getValue();
                if (i > index) {
                    commentLines.put((short) (i - 1), commentLineList);
                } else if (i < index) {
                    commentLines.put(i, commentLineList);
                }
            }
            this.commentLines.put(replacesType, commentLines);
        }
        replaces.get(replacesType).remove(index);
        edited = true;
        saveConfig();
    }

    public int checkReplaceKey(@Nonnull String key, @Nonnull ReplacesType replacesType) {
        for (int i = 0; i < replaces.size(); i++) {
            if (replaces.get(replacesType).get(i).equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private boolean checkComment(String key, String value, Pattern pattern, @Nonnull ReplacesType replacesType) {
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            LinkedList<CommentLine> commentLines = this.commentLines.get(replacesType).get((short) replaces.size());
            if (commentLines == null) {
                this.commentLines.get(replacesType).put((short) replaces.size(), new LinkedList<>(Collections.singletonList(new CommentLine(key, value))));
            } else {
                commentLines.add(new CommentLine(key, value));
            }
            return true;
        }
        return false;
    }

    private void loadData(File file, DotYamlConfiguration configuration) {
        this.configuration = configuration;
        this.file = file;
        enable = configuration.getBoolean("Options鰠Enable", false);
        priority = configuration.getInt("Options鰠Priority", 5);
        author = configuration.getString("Options鰠Author");
        version = configuration.getString("Options鰠Version");
        List<String> types = configuration.getStringList("Options鰠Filter鰠Listen-Types");
        boolean typeFound;
        if (types.isEmpty()) {
            ListenType[] listenTypes = ListenType.values();
            listenTypeList.addAll(Arrays.asList(listenTypes));
        } else {
            for (String type : types) {
                typeFound = false;
                for (var listenType : ListenType.values()) {
                    if (listenType.getName().equalsIgnoreCase(type)) {
                        typeFound = true;
                        listenTypeList.add(listenType);
                        break;
                    }
                }
                if (!typeFound) {
                    Bukkit.getConsoleSender().sendMessage("§7[§cProtocol§6StringReplacer§7] §c未知或不支持的监听类型: " + type);
                }
            }
        }

        String matchType = configuration.getString("Options鰠Match-Type", "Contain");
        typeFound = false;
        for (MatchType availableMatchType : MatchType.values()) {
            if (availableMatchType.name.equalsIgnoreCase(matchType)) {
                this.matchType = availableMatchType;
                typeFound = true;
                break;
            }
        }
        if (!typeFound) {
            this.matchType = MatchType.CONTAIN;
            Bukkit.getConsoleSender().sendMessage("§7[§cProtocol§6StringReplacer§7] §c未知的文本匹配方式: " + matchType + ". 使用默认值\"Contain\"");
        }
        ConfigurationSection section = configuration.getConfigurationSection("Replaces鰠Common");
        if (section != null) {
            replaces.put(ReplacesType.COMMON, new ListOrderedMap());
            commentLines.put(ReplacesType.COMMON, new HashMap<>());
            Pattern commentKeyPattern = CommentYamlConfiguration.getCommentKeyPattern();
            if (this.matchType == MatchType.REGEX) {
                for (String key : section.getKeys(true)) {
                    String value = section.getString(key);
                    if (!checkComment(key, value, commentKeyPattern, ReplacesType.COMMON)) {
                        replaces.get(ReplacesType.COMMON).put(Pattern.compile(key), value);
                    }
                }
            } else {
                for (String key : section.getKeys(true)) {
                    String value = section.getString(key);
                    if (!checkComment(key, value, commentKeyPattern, ReplacesType.COMMON)) {
                        replaces.get(ReplacesType.COMMON).put(key, value);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "ReplacerConfig{" +
                "file=" + file +
                ", configuration=" + configuration +
                ", enable=" + enable +
                ", priority=" + priority +
                ", listenTypeList=" + listenTypeList +
                ", matchType=" + matchType +
                ", replaces=" + replaces +
                ", commentLines=" + commentLines +
                ", author='" + author + '\'' +
                ", version='" + version + '\'' +
                ", edited=" + edited +
                '}';
    }

}
