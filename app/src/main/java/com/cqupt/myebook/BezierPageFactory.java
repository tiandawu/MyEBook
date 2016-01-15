package com.cqupt.myebook;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * Created by tiandawu on 2015/12/9.
 */
public class BezierPageFactory {

    private int mWidth;
    private int mHeight;
    private File book_file;
    private int mBufferLen;
    private MappedByteBuffer mBuffer;
    private String m_strCharsetName = "GBK";
    private int mLineCount; // 每页可以显示的行数
    private int m_mbBufBegin;
    private int m_mbBufEnd;
    private float mVisibleHeight; // 绘制内容的高
    private float mVisibleWidth; // 绘制内容的宽
    private Paint mPaint;
    private boolean m_isfirstPage, m_islastPage;
    private Vector<String> m_lines = new Vector<>();
    private boolean bIsUserBg = false;

    private OnProgressChangedListener onProgressChangedListener;

    private Bitmap m_book_bg;
    private int m_backColor = 0xffff9e85; // 背景颜色
    private int marginWidth = 35; // 左右与边缘的距离
    private int marginHeight = 80; // 上下与边缘的距离
    private int m_fontSize = 60;
    private int mlineMargin = 5;
    private int m_textColor = Color.BLACK;

    public BezierPageFactory(int w, int h, int backColor, int textColor, int line_jianju, int fontSize, int jindu) {
        mWidth = w;
        mHeight = h;
        m_textColor = textColor;
        m_backColor = backColor;
        mlineMargin = line_jianju;
        m_fontSize = fontSize;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.setTextSize(m_fontSize);
        mPaint.setColor(m_textColor);
        mPaint.setTypeface(null);
        mVisibleWidth = mWidth - marginWidth * 2;
        mVisibleHeight = mHeight - marginHeight * 2 + 108;
        mLineCount = (int) (mVisibleHeight / (m_fontSize + mlineMargin)); // 可显示的行数
        m_mbBufBegin = m_mbBufEnd = jindu;
    }

    /**
     * 打开文件
     *
     * @param strFilePath file路径
     * @throws IOException
     */
    public void openBook(String strFilePath) throws IOException {
        book_file = new File(strFilePath);
        long lLen = book_file.length();
        mBufferLen = (int) lLen;
        mBuffer = new RandomAccessFile(book_file, "r").getChannel().map(FileChannel.MapMode.READ_ONLY, 0, lLen);
        m_strCharsetName = codeString(strFilePath);
    }


    /**
     * 判断文件的编码格式
     *
     * @param fileName :file
     * @return 文件编码格式
     */
    public static String codeString(String fileName) {
        BufferedInputStream bin;
        int p = 0;
        String code;
        try {
            bin = new BufferedInputStream(new FileInputStream(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            bin = null;
        }
        if (bin != null) {
            try {
                p = (bin.read() << 8) + bin.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        switch (p) {
            case 0xefbb:
                code = "UTF-8";
                break;
            case 0xfffe:
                code = "Unicode";
                break;
            case 0xfeff:
                code = "UTF-16BE";
                break;
            default:
                code = "GBK";
        }
        return code;
    }


    /**
     * 读取后一段落
     *
     * @param nFromPos
     * @return
     */
    protected byte[] readParagraphBack(int nFromPos) {
        int nEnd = nFromPos;
        int i;
        byte b0, b1;
        if (m_strCharsetName.equals("UTF-16LE")) {
            i = nEnd - 2;
            while (i > 0) {
                b0 = mBuffer.get(i);
                b1 = mBuffer.get(i + 1);
                if (b0 == 0x0a && b1 == 0x00 && i != nEnd - 2) {
                    i += 2;
                    break;
                }
                i--;
            }

        } else if (m_strCharsetName.equals("UTF-16BE")) {
            i = nEnd - 2;
            while (i > 0) {
                b0 = mBuffer.get(i);
                b1 = mBuffer.get(i + 1);
                if (b0 == 0x00 && b1 == 0x0a && i != nEnd - 2) {
                    i += 2;
                    break;
                }
                i--;
            }
        } else {
            i = nEnd - 1;
            while (i > 0) {
                b0 = mBuffer.get(i);
                if (b0 == 0x0a && i != nEnd - 1) {
                    i++;
                    break;
                }
                i--;
            }
        }
        if (i < 0)
            i = 0;
        int nParaSize = nEnd - i;
        int j;
        byte[] buf = new byte[nParaSize];
        for (j = 0; j < nParaSize; j++) {
            buf[j] = mBuffer.get(i + j);
        }
        return buf;
    }


    /**
     * 读取前以段落
     *
     * @param nFromPos
     * @return
     */
    protected byte[] readParagraphForward(int nFromPos) {
        int nStart = nFromPos;
        int i = nStart;
        byte b0, b1;
        // 根据编码格式判断换行
        if (m_strCharsetName.equals("UTF-16LE")) {
            while (i < mBufferLen - 1) {
                b0 = mBuffer.get(i++);
                b1 = mBuffer.get(i++);
                if (b0 == 0x0a && b1 == 0x00) {
                    break;
                }
            }
        } else if (m_strCharsetName.equals("UTF-16BE")) {
            while (i < mBufferLen - 1) {
                b0 = mBuffer.get(i++);
                b1 = mBuffer.get(i++);
                if (b0 == 0x00 && b1 == 0x0a) {
                    break;
                }
            }
        } else {
            while (i < mBufferLen) {
                b0 = mBuffer.get(i++);
                if (b0 == 0x0a) {
                    break;
                }
            }
        }
        int nParaSize = i - nStart;
        byte[] buf = new byte[nParaSize];
        for (i = 0; i < nParaSize; i++) {
            buf[i] = mBuffer.get(nFromPos + i);
        }
        return buf;
    }


    protected Vector<String> pageDown() {
        String strParagraph = "";
        Vector<String> lines = new Vector<>();
        while (lines.size() < mLineCount && m_mbBufEnd < mBufferLen) {
            byte[] paraBuf = readParagraphForward(m_mbBufEnd); // 读取一个段落
            m_mbBufEnd += paraBuf.length;
            try {
                strParagraph = new String(paraBuf, m_strCharsetName);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String strReturn = "";
            if (strParagraph.indexOf("\r\n") != -1) {
                strReturn = "\r\n";
                strParagraph = strParagraph.replaceAll("\r\n", "");
            } else if (strParagraph.indexOf("\n") != -1) {
                strReturn = "\n";
                strParagraph = strParagraph.replaceAll("\n", "");
            }

            if (strParagraph.length() == 0) {
                lines.add(strParagraph);
            }
            while (strParagraph.length() > 0) {
                int nSize = mPaint.breakText(strParagraph, true, mVisibleWidth,
                        null);
                lines.add(strParagraph.substring(0, nSize));
                strParagraph = strParagraph.substring(nSize);
                if (lines.size() >= mLineCount) {
                    break;
                }
            }
            if (strParagraph.length() != 0) {
                try {
                    m_mbBufEnd -= (strParagraph + strReturn)
                            .getBytes(m_strCharsetName).length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return lines;
    }

    protected void pageUp() {
        if (m_mbBufBegin < 0)
            m_mbBufBegin = 0;
        Vector<String> lines = new Vector<>();
        String strParagraph = "";
        while (lines.size() < mLineCount && m_mbBufBegin > 0) {
            Vector<String> paraLines = new Vector<>();
            byte[] paraBuf = readParagraphBack(m_mbBufBegin);
            m_mbBufBegin -= paraBuf.length;
            try {
                strParagraph = new String(paraBuf, m_strCharsetName);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            strParagraph = strParagraph.replaceAll("\r\n", "");
            strParagraph = strParagraph.replaceAll("\n", "");

            if (strParagraph.length() == 0) {
                paraLines.add(strParagraph);
            }
            while (strParagraph.length() > 0) {
                int nSize = mPaint.breakText(strParagraph, true, mVisibleWidth,
                        null);
                paraLines.add(strParagraph.substring(0, nSize));
                strParagraph = strParagraph.substring(nSize);
            }
            lines.addAll(0, paraLines);
        }
        while (lines.size() > mLineCount) {
            try {
                m_mbBufBegin += lines.get(0).getBytes(m_strCharsetName).length;
                lines.remove(0);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        m_mbBufEnd = m_mbBufBegin;
        return;
    }

    protected void prePage() throws IOException {
        if (m_mbBufBegin <= 0) {
            m_mbBufBegin = 0;
            m_isfirstPage = true;
            return;
        } else
            m_isfirstPage = false;
        m_islastPage = false;
        m_lines.clear();
        pageUp();
        m_lines = pageDown();
    }

    public void nextPage() throws IOException {
        if (m_mbBufEnd >= mBufferLen) {
            m_islastPage = true;
            return;
        } else
            m_islastPage = false;
        m_isfirstPage = false;
        m_lines.clear();
        m_mbBufBegin = m_mbBufEnd;
        m_lines = pageDown();
    }

    public void Draw(Canvas c) {
        if (m_lines.size() == 0)
            m_lines = pageDown();
        if (m_lines.size() > 0) {
            if (bIsUserBg && m_book_bg != null)
                c.drawBitmap(m_book_bg, 0, 0, null);

            else {
                c.drawColor(m_backColor);
            }
            int y = marginHeight + m_fontSize;
            for (String strLine : m_lines) {

                c.drawText(strLine, marginWidth, y, mPaint);
                y += m_fontSize + mlineMargin;
            }
        }
        float fPercent = (float) (m_mbBufBegin * 1.0 / mBufferLen);//阅读第一页的进度为0.0%
//        float fPercent = (float) (m_mbBufEnd * 1.0 / m_mbBufLen);
        DecimalFormat df = new DecimalFormat("#0.0");
        String strPercent = df.format(fPercent * 100) + "%";

        onProgressChangedListener.setProgress(strPercent);
    }

    public void setBgBitmap(Bitmap BG) {
        m_book_bg = BG;

        Matrix matrix = new Matrix();
        int width = m_book_bg.getWidth();// 获取资源位图的宽
        int height = m_book_bg.getHeight();// 获取资源位图的高
        float w = (float) mWidth / (float) m_book_bg.getWidth();
        float h = (float) mHeight / (float) m_book_bg.getHeight();
        matrix.postScale(w, h);// 获取缩放比例
        m_book_bg = Bitmap.createBitmap(m_book_bg, 0, 0, width, height, matrix,
                true);// 根据缩放比例获取新的位图
    }

    public void setUseBg(boolean is) {
        bIsUserBg = is;
    }

    public boolean isfirstPage() {
        return m_isfirstPage;
    }

    public boolean islastPage() {
        return m_islastPage;
    }

    public int get_jindu() {
        return m_mbBufEnd;
    }

    public float get_jindu_baifen() {
        return (float) (m_mbBufBegin * 1.0 / mBufferLen);
    }

    public void set_jindu(int jindu) {
        if (jindu < 0) {
            m_mbBufBegin = m_mbBufEnd = 0;
        } else if (jindu > mBufferLen) {
            m_mbBufBegin = m_mbBufEnd = mBufferLen;
        } else {
            m_mbBufBegin = m_mbBufEnd = jindu;
        }
        pageUp();
        if (m_mbBufBegin != 0) {
            m_lines.clear();
            m_lines = pageDown();

            m_mbBufBegin = m_mbBufEnd;
        }
        m_lines.clear();
        m_lines = pageDown();
    }

    public void set_jindu_baifen(int jindu) {

        if (jindu < 0) {
            m_mbBufBegin = m_mbBufEnd = 0;
        } else if (jindu > 100) {
            m_mbBufBegin = m_mbBufEnd = mBufferLen;
        } else {
            m_mbBufBegin = m_mbBufEnd = (int) (((float) jindu / 100) * mBufferLen);
        }

        pageUp();
        if (m_mbBufBegin != 0) {
            m_lines.clear();
            m_lines = pageDown();

            m_mbBufBegin = m_mbBufEnd;
        }
        m_lines.clear();
        m_lines = pageDown();
    }

    public void setTextColor(int color) {
        mPaint.setColor(color);
    }

    /**
     * 设置接口
     *
     * @param onProgressChangedListener
     */
    public void setOnProgressChangedListener(OnProgressChangedListener onProgressChangedListener) {
        this.onProgressChangedListener = onProgressChangedListener;
    }

    /**
     * 设置阅读进度的接口
     */
    public interface OnProgressChangedListener {
        /**
         * 设置进度方法
         *
         * @param progress 进度值
         */
        void setProgress(String progress);
    }
}
