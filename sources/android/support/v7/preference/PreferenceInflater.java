package android.support.v7.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.InflateException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class PreferenceInflater {
    private static final HashMap<String, Constructor> CONSTRUCTOR_MAP = new HashMap<>();
    private static final Class<?>[] CONSTRUCTOR_SIGNATURE = {Context.class, AttributeSet.class};
    private static final String EXTRA_TAG_NAME = "extra";
    private static final String INTENT_TAG_NAME = "intent";
    private static final String TAG = "PreferenceInflater";
    private final Object[] mConstructorArgs = new Object[2];
    private final Context mContext;
    private String[] mDefaultPackages;
    private PreferenceManager mPreferenceManager;

    public PreferenceInflater(Context context, PreferenceManager preferenceManager) {
        this.mContext = context;
        init(preferenceManager);
    }

    private void init(PreferenceManager preferenceManager) {
        this.mPreferenceManager = preferenceManager;
        setDefaultPackages(new String[]{"android.support.v14.preference.", "android.support.v7.preference."});
    }

    public void setDefaultPackages(String[] defaultPackage) {
        this.mDefaultPackages = defaultPackage;
    }

    public String[] getDefaultPackages() {
        return this.mDefaultPackages;
    }

    public Context getContext() {
        return this.mContext;
    }

    public Preference inflate(int resource, @Nullable PreferenceGroup root) {
        XmlResourceParser parser = getContext().getResources().getXml(resource);
        try {
            return inflate((XmlPullParser) parser, root);
        } finally {
            parser.close();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x001a A[Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }] */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x003c A[SYNTHETIC, Splitter:B:18:0x003c] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public android.support.v7.preference.Preference inflate(org.xmlpull.v1.XmlPullParser r12, @android.support.annotation.Nullable android.support.v7.preference.PreferenceGroup r13) {
        /*
            r11 = this;
            r10 = 2
            java.lang.Object[] r7 = r11.mConstructorArgs
            monitor-enter(r7)
            android.util.AttributeSet r0 = android.util.Xml.asAttributeSet(r12)     // Catch:{ all -> 0x0039 }
            java.lang.Object[] r6 = r11.mConstructorArgs     // Catch:{ all -> 0x0039 }
            r8 = 0
            android.content.Context r9 = r11.mContext     // Catch:{ all -> 0x0039 }
            r6[r8] = r9     // Catch:{ all -> 0x0039 }
        L_0x000f:
            int r4 = r12.next()     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            if (r4 == r10) goto L_0x0018
            r6 = 1
            if (r4 != r6) goto L_0x000f
        L_0x0018:
            if (r4 == r10) goto L_0x003c
            android.view.InflateException r6 = new android.view.InflateException     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            r8.<init>()     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            java.lang.String r9 = r12.getPositionDescription()     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            java.lang.StringBuilder r8 = r8.append(r9)     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            java.lang.String r9 = ": No start tag found!"
            java.lang.StringBuilder r8 = r8.append(r9)     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            java.lang.String r8 = r8.toString()     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            r6.<init>(r8)     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            throw r6     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
        L_0x0037:
            r1 = move-exception
            throw r1     // Catch:{ all -> 0x0039 }
        L_0x0039:
            r6 = move-exception
            monitor-exit(r7)     // Catch:{ all -> 0x0039 }
            throw r6
        L_0x003c:
            java.lang.String r6 = r12.getName()     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            android.support.v7.preference.Preference r5 = r11.createItemFromTag(r6, r0)     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            android.support.v7.preference.PreferenceGroup r5 = (android.support.v7.preference.PreferenceGroup) r5     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            android.support.v7.preference.PreferenceGroup r3 = r11.onMergeRoots(r13, r5)     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            r11.rInflate(r12, r3, r0)     // Catch:{ InflateException -> 0x0037, XmlPullParserException -> 0x004f, IOException -> 0x005d }
            monitor-exit(r7)     // Catch:{ all -> 0x0039 }
            return r3
        L_0x004f:
            r1 = move-exception
            android.view.InflateException r2 = new android.view.InflateException     // Catch:{ all -> 0x0039 }
            java.lang.String r6 = r1.getMessage()     // Catch:{ all -> 0x0039 }
            r2.<init>(r6)     // Catch:{ all -> 0x0039 }
            r2.initCause(r1)     // Catch:{ all -> 0x0039 }
            throw r2     // Catch:{ all -> 0x0039 }
        L_0x005d:
            r1 = move-exception
            android.view.InflateException r2 = new android.view.InflateException     // Catch:{ all -> 0x0039 }
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x0039 }
            r6.<init>()     // Catch:{ all -> 0x0039 }
            java.lang.String r8 = r12.getPositionDescription()     // Catch:{ all -> 0x0039 }
            java.lang.StringBuilder r6 = r6.append(r8)     // Catch:{ all -> 0x0039 }
            java.lang.String r8 = ": "
            java.lang.StringBuilder r6 = r6.append(r8)     // Catch:{ all -> 0x0039 }
            java.lang.String r8 = r1.getMessage()     // Catch:{ all -> 0x0039 }
            java.lang.StringBuilder r6 = r6.append(r8)     // Catch:{ all -> 0x0039 }
            java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x0039 }
            r2.<init>(r6)     // Catch:{ all -> 0x0039 }
            r2.initCause(r1)     // Catch:{ all -> 0x0039 }
            throw r2     // Catch:{ all -> 0x0039 }
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.preference.PreferenceInflater.inflate(org.xmlpull.v1.XmlPullParser, android.support.v7.preference.PreferenceGroup):android.support.v7.preference.Preference");
    }

    @NonNull
    private PreferenceGroup onMergeRoots(PreferenceGroup givenRoot, @NonNull PreferenceGroup xmlRoot) {
        if (givenRoot != null) {
            return givenRoot;
        }
        xmlRoot.onAttachedToHierarchy(this.mPreferenceManager);
        return xmlRoot;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0076, code lost:
        r4 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0077, code lost:
        throw r4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x007e, code lost:
        r4 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x007f, code lost:
        r5 = new android.view.InflateException(r14.getPositionDescription() + ": Error inflating class " + r12);
        r5.initCause(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00a2, code lost:
        throw r5;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x007e A[ExcHandler: Exception (r4v0 'e' java.lang.Exception A[CUSTOM_DECLARE]), Splitter:B:2:0x000a] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.support.v7.preference.Preference createItem(@android.support.annotation.NonNull java.lang.String r12, @android.support.annotation.Nullable java.lang.String[] r13, android.util.AttributeSet r14) throws java.lang.ClassNotFoundException, android.view.InflateException {
        /*
            r11 = this;
            java.util.HashMap<java.lang.String, java.lang.reflect.Constructor> r8 = CONSTRUCTOR_MAP
            java.lang.Object r3 = r8.get(r12)
            java.lang.reflect.Constructor r3 = (java.lang.reflect.Constructor) r3
            if (r3 != 0) goto L_0x0029
            android.content.Context r8 = r11.mContext     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.lang.ClassLoader r1 = r8.getClassLoader()     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            r2 = 0
            if (r13 == 0) goto L_0x0016
            int r8 = r13.length     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            if (r8 != 0) goto L_0x0035
        L_0x0016:
            java.lang.Class r2 = r1.loadClass(r12)     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
        L_0x001a:
            java.lang.Class<?>[] r8 = CONSTRUCTOR_SIGNATURE     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.lang.reflect.Constructor r3 = r2.getConstructor(r8)     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            r8 = 1
            r3.setAccessible(r8)     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.util.HashMap<java.lang.String, java.lang.reflect.Constructor> r8 = CONSTRUCTOR_MAP     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            r8.put(r12, r3)     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
        L_0x0029:
            java.lang.Object[] r0 = r11.mConstructorArgs     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            r8 = 1
            r0[r8] = r14     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.lang.Object r8 = r3.newInstance(r0)     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            android.support.v7.preference.Preference r8 = (android.support.v7.preference.Preference) r8     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            return r8
        L_0x0035:
            r6 = 0
            int r9 = r13.length     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            r8 = 0
        L_0x0038:
            if (r8 >= r9) goto L_0x0051
            r7 = r13[r8]     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch:{ ClassNotFoundException -> 0x0078, Exception -> 0x007e }
            r10.<init>()     // Catch:{ ClassNotFoundException -> 0x0078, Exception -> 0x007e }
            java.lang.StringBuilder r10 = r10.append(r7)     // Catch:{ ClassNotFoundException -> 0x0078, Exception -> 0x007e }
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch:{ ClassNotFoundException -> 0x0078, Exception -> 0x007e }
            java.lang.String r10 = r10.toString()     // Catch:{ ClassNotFoundException -> 0x0078, Exception -> 0x007e }
            java.lang.Class r2 = r1.loadClass(r10)     // Catch:{ ClassNotFoundException -> 0x0078, Exception -> 0x007e }
        L_0x0051:
            if (r2 != 0) goto L_0x001a
            if (r6 != 0) goto L_0x007d
            android.view.InflateException r8 = new android.view.InflateException     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            r9.<init>()     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.lang.String r10 = r14.getPositionDescription()     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.lang.StringBuilder r9 = r9.append(r10)     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.lang.String r10 = ": Error inflating class "
            java.lang.StringBuilder r9 = r9.append(r10)     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.lang.StringBuilder r9 = r9.append(r12)     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            java.lang.String r9 = r9.toString()     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            r8.<init>(r9)     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
            throw r8     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
        L_0x0076:
            r4 = move-exception
            throw r4
        L_0x0078:
            r4 = move-exception
            r6 = r4
            int r8 = r8 + 1
            goto L_0x0038
        L_0x007d:
            throw r6     // Catch:{ ClassNotFoundException -> 0x0076, Exception -> 0x007e }
        L_0x007e:
            r4 = move-exception
            android.view.InflateException r5 = new android.view.InflateException
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            java.lang.String r9 = r14.getPositionDescription()
            java.lang.StringBuilder r8 = r8.append(r9)
            java.lang.String r9 = ": Error inflating class "
            java.lang.StringBuilder r8 = r8.append(r9)
            java.lang.StringBuilder r8 = r8.append(r12)
            java.lang.String r8 = r8.toString()
            r5.<init>(r8)
            r5.initCause(r4)
            throw r5
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.preference.PreferenceInflater.createItem(java.lang.String, java.lang.String[], android.util.AttributeSet):android.support.v7.preference.Preference");
    }

    /* access modifiers changed from: protected */
    public Preference onCreateItem(String name, AttributeSet attrs) throws ClassNotFoundException {
        return createItem(name, this.mDefaultPackages, attrs);
    }

    private Preference createItemFromTag(String name, AttributeSet attrs) {
        try {
            if (-1 == name.indexOf(46)) {
                return onCreateItem(name, attrs);
            }
            return createItem(name, (String[]) null, attrs);
        } catch (InflateException e) {
            throw e;
        } catch (ClassNotFoundException e2) {
            InflateException ie = new InflateException(attrs.getPositionDescription() + ": Error inflating class (not found)" + name);
            ie.initCause(e2);
            throw ie;
        } catch (Exception e3) {
            InflateException ie2 = new InflateException(attrs.getPositionDescription() + ": Error inflating class " + name);
            ie2.initCause(e3);
            throw ie2;
        }
    }

    private void rInflate(XmlPullParser parser, Preference parent, AttributeSet attrs) throws XmlPullParserException, IOException {
        int depth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                return;
            }
            if (type == 2) {
                String name = parser.getName();
                if (INTENT_TAG_NAME.equals(name)) {
                    try {
                        parent.setIntent(Intent.parseIntent(getContext().getResources(), parser, attrs));
                    } catch (IOException e) {
                        XmlPullParserException ex = new XmlPullParserException("Error parsing preference");
                        ex.initCause(e);
                        throw ex;
                    }
                } else if (EXTRA_TAG_NAME.equals(name)) {
                    getContext().getResources().parseBundleExtra(EXTRA_TAG_NAME, attrs, parent.getExtras());
                    try {
                        skipCurrentTag(parser);
                    } catch (IOException e2) {
                        XmlPullParserException ex2 = new XmlPullParserException("Error parsing preference");
                        ex2.initCause(e2);
                        throw ex2;
                    }
                } else {
                    Preference item = createItemFromTag(name, attrs);
                    ((PreferenceGroup) parent).addItemFromInflater(item);
                    rInflate(parser, item, attrs);
                }
            }
        }
    }

    private static void skipCurrentTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
        }
    }
}
