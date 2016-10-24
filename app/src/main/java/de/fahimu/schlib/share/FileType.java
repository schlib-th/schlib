/*
 * FileType.java
 *
 * Copyright 2015 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.share;

import de.fahimu.android.share.ExternalFile;
import de.fahimu.schlib.app.R;

/**
 * Supported types of an {@link ExternalFile}.
 */
public enum FileType implements ExternalFile.Type {

   BACKUP(R.string.external_file_backup),
   PRINTS(R.string.external_file_prints),
   IMPORT(R.string.external_file_import);

   private final int resId;

   public int getResId() { return resId; }

   FileType(int resId) { this.resId = resId; }

}