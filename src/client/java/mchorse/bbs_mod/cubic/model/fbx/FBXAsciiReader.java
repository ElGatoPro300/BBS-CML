package mchorse.bbs_mod.cubic.model.fbx;

import java.util.ArrayList;
import java.util.List;

public class FBXAsciiReader
{
    private final String text;
    private int pos;
    private final int len;

    public FBXAsciiReader(String text)
    {
        this.text = text;
        this.len = text.length();
    }

    public FBXNode read()
    {
        FBXNode root = new FBXNode("__root__");

        this.skipWhitespaceAndComments();

        while (this.pos < this.len)
        {
            FBXNode node = this.readNode();

            if (node == null)
            {
                break;
            }

            root.children.add(node);
            this.skipWhitespaceAndComments();
        }

        return root;
    }

    private FBXNode readNode()
    {
        this.skipWhitespaceAndComments();

        if (this.pos >= this.len || this.peek() == '}')
        {
            return null;
        }

        String name = this.readIdentifier();

        this.skipWhitespaceAndComments();

        if (this.pos < this.len && this.peek() == ':')
        {
            this.pos++;
        }

        FBXNode node = new FBXNode(name);

        this.skipInlineWhitespace();

        while (this.pos < this.len)
        {
            char c = this.peek();

            if (c == '\n' || c == '\r' || c == ';' || c == '{')
            {
                break;
            }

            Object value = this.readValue();
            node.properties.add(value);

            this.skipInlineWhitespace();

            if (this.pos < this.len && this.peek() == ',')
            {
                this.pos++;
                this.skipInlineWhitespace();
            } else
            {
                break;
            }
        }

        this.skipWhitespaceAndComments();

        if (this.pos < this.len && this.peek() == '{')
        {
            this.pos++;
            this.skipWhitespaceAndComments();

            while (this.pos < this.len && this.peek() != '}')
            {
                FBXNode child = this.readNode();

                if (child == null)
                {
                    break;
                }

                node.children.add(child);
                this.skipWhitespaceAndComments();
            }

            if (this.pos < this.len && this.peek() == '}')
            {
                this.pos++;
            }
        }

        return node;
    }

    private Object readValue()
    {
        char c = this.peek();

        if (c == '"')
        {
            return this.readQuotedString();
        }

        int start = this.pos;

        while (this.pos < this.len)
        {
            char ch = this.peek();

            if (ch == ',' || ch == '\n' || ch == '\r' || ch == '{' || ch == ';')
            {
                break;
            }

            this.pos++;
        }

        String token = this.text.substring(start, this.pos).trim();

        return this.parseScalar(token);
    }

    private Object parseScalar(String token)
    {
        if (token.isEmpty())
        {
            return token;
        }

        char first = token.charAt(0);

        if (first == '*')
        {
            /* array-length marker like "*12", value itself is just the count, nothing else */
            try
            {
                return Long.parseLong(token.substring(1).trim());
            } catch (NumberFormatException e)
            {
                return token;
            }
        }

        try
        {
            if (token.indexOf('.') >= 0 || token.indexOf('e') > 0 || token.indexOf('E') > 0)
            {
                return Double.parseDouble(token);
            }

            return Long.parseLong(token);
        } catch (NumberFormatException e)
        {
            return token;
        }
    }

    private String readQuotedString()
    {
        this.pos++;
        int start = this.pos;

        while (this.pos < this.len && this.peek() != '"')
        {
            this.pos++;
        }

        String s = this.text.substring(start, this.pos);

        if (this.pos < this.len)
        {
            this.pos++;
        }

        return s;
    }

    private String readIdentifier()
    {
        int start = this.pos;

        while (this.pos < this.len)
        {
            char c = this.peek();

            if (Character.isWhitespace(c) || c == ':' || c == ',' || c == '{' || c == '}' || c == ';')
            {
                break;
            }

            this.pos++;
        }

        return this.text.substring(start, this.pos);
    }

    private void skipInlineWhitespace()
    {
        while (this.pos < this.len)
        {
            char c = this.peek();

            if (c == ' ' || c == '\t')
            {
                this.pos++;
            } else
            {
                break;
            }
        }
    }

    private void skipWhitespaceAndComments()
    {
        while (this.pos < this.len)
        {
            char c = this.peek();

            if (Character.isWhitespace(c))
            {
                this.pos++;
            } else if (c == ';')
            {
                while (this.pos < this.len && this.peek() != '\n')
                {
                    this.pos++;
                }
            } else
            {
                break;
            }
        }
    }

    private char peek()
    {
        return this.text.charAt(this.pos);
    }

    public static List<FBXNode> flattenArrayProperties(FBXNode arrayHolderNode)
    {
        List<FBXNode> aNodes = arrayHolderNode.childrenNamed("a");
        List<FBXNode> out = new ArrayList<FBXNode>();
        out.addAll(aNodes);

        return out;
    }
}
