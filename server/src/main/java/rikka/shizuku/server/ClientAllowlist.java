package rikka.shizuku.server;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import moe.shizuku.server.BuildConfig;
import rikka.hidden.compat.PackageManagerApis;
import rikka.shizuku.server.util.Android17Compat;
import rikka.shizuku.server.util.Logger;
import rikka.shizuku.server.util.UserHandleCompat;

/**
 * WhtsAgent Helper serves only WhtsAgent's own apps. A client is allowed when its package name is in
 * {@code allowedClientPackages} AND its current signing certificate's SHA-256 is in
 * {@code allowedClientCertSha256} (both in gradle/libs.versions.toml). Allowed clients never see a
 * permission prompt; every other app is refused, whatever its saved config or runtime grant says.
 */
public final class ClientAllowlist {

    private static final Logger LOGGER = new Logger("ClientAllowlist");

    private static final Set<String> PACKAGES = split(BuildConfig.ALLOWED_CLIENT_PACKAGES, false);
    // Digests compare as lowercase hex; colons from `apksigner --print-certs` output are tolerated.
    private static final Set<String> CERTS = split(BuildConfig.ALLOWED_CLIENT_CERTS.replace(":", ""), true);

    private ClientAllowlist() {
    }

    public static boolean isAllowed(String packageName, int userId) {
        if (packageName == null || !PACKAGES.contains(packageName)) {
            return false;
        }
        for (String digest : signerDigests(packageName, userId)) {
            if (CERTS.contains(digest)) {
                return true;
            }
        }
        LOGGER.w("%s is not signed with an allowed certificate", packageName);
        return false;
    }

    /** True when any package sharing {@code uid} is allowed. */
    public static boolean isAllowedUid(int uid) {
        int userId = UserHandleCompat.getUserId(uid);
        for (String packageName : PackageManagerApis.getPackagesForUidNoThrow(uid)) {
            if (isAllowed(packageName, userId)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> signerDigests(String packageName, int userId) {
        Signature[] signers = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo pi = Android17Compat.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES, userId);
                SigningInfo signingInfo = pi != null ? pi.signingInfo : null;
                // Current signer(s) only: a key the app has rotated away from must not keep working.
                signers = signingInfo != null ? signingInfo.getApkContentsSigners() : null;
            } else {
                PackageInfo pi = Android17Compat.getPackageInfo(packageName, PackageManager.GET_SIGNATURES, userId);
                signers = pi != null ? pi.signatures : null;
            }
        } catch (Throwable tr) {
            LOGGER.w(tr, "read signatures of %s", packageName);
        }
        if (signers == null) {
            return Collections.emptyList();
        }
        List<String> digests = new ArrayList<>(signers.length);
        for (Signature signer : signers) {
            digests.add(sha256(signer.toByteArray()));
        }
        return digests;
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static Set<String> split(String csv, boolean lowercase) {
        Set<String> set = new HashSet<>();
        for (String part : csv.split(",")) {
            String value = lowercase ? part.trim().toLowerCase(Locale.ROOT) : part.trim();
            if (!value.isEmpty()) {
                set.add(value);
            }
        }
        return set;
    }
}
