package mchorse.bbs_mod.utils;

public class NaturalOrderComparator
{
    public static int compare(boolean caseInsensitive, String a, String b)
    {
        int indexA = 0;
        int indexB = 0;
        int lenA = a.length();
        int lenB = b.length();

        while (indexA < lenA && indexB < lenB)
        {
            String chunkA = getChunk(a, lenA, indexA);
            String chunkB = getChunk(b, lenB, indexB);
            char charA = a.charAt(indexA);
            char charB = b.charAt(indexB);

            indexA += chunkA.length();
            indexB += chunkB.length();

            int result;

            if (isDigit(charA) && isDigit(charB))
            {
                result = compareNumericChunks(caseInsensitive, chunkA, chunkB);
            }
            else
            {
                result = caseInsensitive ? chunkA.compareToIgnoreCase(chunkB) : chunkA.compareTo(chunkB);
            }

            if (result != 0)
            {
                return result;
            }
        }

        return lenA - lenB;
    }

    private static String getChunk(String s, int length, int marker)
    {
        StringBuilder chunk = new StringBuilder();
        char c = s.charAt(marker);

        chunk.append(c);

        marker += 1;

        if (isDigit(c))
        {
            while (marker < length)
            {
                c = s.charAt(marker);

                if (!isDigit(c))
                {
                    break;
                }

                chunk.append(c);
                marker++;
            }
        }
        else
        {
            while (marker < length)
            {
                c = s.charAt(marker);

                if (isDigit(c))
                {
                    break;
                }

                chunk.append(c);
                marker++;
            }
        }

        return chunk.toString();
    }

    private static boolean isDigit(char ch)
    {
        return ch >= '0' && ch <= '9';
    }

    private static int compareNumericChunks(boolean caseInsensitive, String num1, String num2)
    {
        num1 = num1.replaceFirst("^0+", "");
        num2 = num2.replaceFirst("^0+", "");

        if (num1.length() != num2.length())
        {
            return num1.length() - num2.length();
        }

        return caseInsensitive ? num1.compareToIgnoreCase(num2) : num1.compareTo(num2);
    }
}