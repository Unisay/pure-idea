package in.twbs.pure.lang.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.WhitespacesAndCommentsBinder;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import in.twbs.pure.lang.psi.PureTokens;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ParserContext {
    @NotNull
    private final PsiBuilder builder;
    private int column;
    private Stack<Integer> indentationLevel = new Stack<Integer>();

    public boolean eof() {
        return builder.eof();
    }

    private final class PureMarker implements PsiBuilder.Marker {
        private final int start;
        private final PsiBuilder.Marker marker;

        protected PureMarker(@NotNull PsiBuilder.Marker marker) {
            this.start = column;
            this.marker = marker;
        }

        public PureMarker(int start, PsiBuilder.Marker marker) {
            this.start = start;
            this.marker = marker;
        }

        @Override
        public PsiBuilder.Marker precede() {
            return new PureMarker(start, marker);
        }

        @Override
        public void drop() {
            marker.drop();
        }

        @Override
        public void rollbackTo() {
            column = start;
            marker.rollbackTo();
        }

        @Override
        public void done(IElementType type) {
            marker.done(type);
        }

        @Override
        public void collapse(IElementType type) {
            marker.collapse(type);
        }

        @Override
        public void doneBefore(IElementType type, PsiBuilder.Marker before) {
            marker.doneBefore(type, before);
        }

        @Override
        public void doneBefore(IElementType type, PsiBuilder.Marker before, String errorMessage) {
            marker.doneBefore(type, before, errorMessage);
        }

        @Override
        public void error(String message) {
            marker.error(message);
        }

        @Override
        public void errorBefore(String message, PsiBuilder.Marker before) {
            marker.errorBefore(message, before);
        }

        @Override
        public void setCustomEdgeTokenBinders(@Nullable WhitespacesAndCommentsBinder left, @Nullable WhitespacesAndCommentsBinder right) {
            marker.setCustomEdgeTokenBinders(left, right);
        }
    }

    public ParserContext(@NotNull PsiBuilder builder) {
        this.builder = builder;
        this.indentationLevel.push(0);
    }

    public void whiteSpace() {
        while (!builder.eof()) {
            IElementType type = builder.getTokenType();
            if (type == PureTokens.WS) {
                advance();
            } else {
                break;
            }
        }
    }

    public void advance() {
        String text = builder.getTokenText();
        if (text != null) {
            IElementType type = builder.getTokenType();
            if (type == PureTokens.STRING || type == PureTokens.WS) {
                for (int i = 0; i < text.length(); i++) {
                    char ch = text.charAt(i);
                    if (ch == '\n') {
                        column = 0;
                    } else if (ch == '\t') {
                        column = column - column % 8 + 8;
                    } else {
                        column++;
                    }
                }
            } else {
                column += text.length();
            }
        }
        builder.advanceLexer();
    }

    @NotNull
    public String text() {
        String text = builder.getTokenText();
        if (text == null) return "";
        return text;
    }

    public boolean match(@NotNull IElementType type) {
        return builder.getTokenType() == type;
    }

    public boolean eat(@NotNull IElementType type) {
        if (builder.getTokenType() == type) {
            advance();
            return true;
        }
        return false;
    }

    public boolean expect(@NotNull IElementType type) {
        PsiBuilder.Marker mark = builder.mark();
        if (builder.getTokenType() == type) {
            advance();
            mark.drop();
            return true;
        }
        mark.error(String.format("Expecting %s.", type.toString()));
        return false;
    }

    @NotNull
    public PsiBuilder.Marker start() {
        // Consume all the white spaces.
        builder.eof();
        return new PureMarker(builder.mark());
    }

    public int getPosition() {
        return builder.getCurrentOffset();
    }

    public int getColumn() {
        return column;
    }

    public int getIndentationLevel() {
        return indentationLevel.peek();
    }

    public int getLastIndentationLevel() {
        if (indentationLevel.size() >= 2) {
            return indentationLevel.get(indentationLevel.size() - 2);
        }
        return 0;
    }

    public void pushIndentationLevel() {
        indentationLevel.push(column);
    }

    public void popIndentationLevel() {
        indentationLevel.tryPop();
    }
}
