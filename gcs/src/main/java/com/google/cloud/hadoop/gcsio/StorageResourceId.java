/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.hadoop.gcsio;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.Objects;

/**
 * Data struct representing either a GCS StorageObject, a GCS Bucket or the GCS root (gs://).
 * If both bucketName and objectName are null, the StorageResourceId refers to GCS root (gs://).
 * If bucketName is non-null, and objectName is null, then this refers to a GCS Bucket. Otherwise,
 * if bucketName and objectName are both non-null, this refers to a GCS StorageObject.
 */
public class StorageResourceId {
  // The singleton instance identifying the GCS root (gs://). Both getObjectName() and
  // getBucketName() will return null.
  public static final StorageResourceId ROOT = new StorageResourceId();

  // Bucket name of this storage resource to be used with the Google Cloud Storage API.
  private final String bucketName;

  // Object name of this storage resource to be used with the Google Cloud Storage API.
  private final String objectName;

  // Human-readable String to be returned by toString(); kept as 'final' member for efficiency.
  private final String readableString;

  /**
   * Constructor for a StorageResourceId which refers to the GCS root (gs://). Private because
   * all external users should just use the singleton StorageResourceId.ROOT.
   */
  private StorageResourceId() {
    this.bucketName = null;
    this.objectName = null;
    this.readableString = createReadableString(bucketName, objectName);
  }

  /**
   * Constructor for a StorageResourceId representing a Bucket; {@code getObjectName()} will return
   * null for a StorageResourceId which represents a Bucket.
   *
   * @param bucketName The bucket name of the resource. Must be non-empty and non-null.
   */
  public StorageResourceId(String bucketName) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(bucketName),
        "bucketName must not be null or empty");

    this.bucketName = bucketName;
    this.objectName = null;
    this.readableString = createReadableString(bucketName, objectName);
  }

  /**
   * Constructor for a StorageResourceId representing a full StorageObject, including bucketName
   * and objectName.
   *
   * @param bucketName The bucket name of the resource. Must be non-empty and non-null.
   * @param objectName The object name of the resource. Must be non-empty and non-null.
   */
  public StorageResourceId(String bucketName, String objectName) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(bucketName),
        "bucketName must not be null or empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(objectName),
        "objectName must not be null or empty");

    this.bucketName = bucketName;
    this.objectName = objectName;
    this.readableString = createReadableString(bucketName, objectName);
  }

  /**
   * Returns true if this StorageResourceId represents a GCS StorageObject; if true, both
   * {@code getBucketName} and {@code getObjectName} will be non-empty and non-null.
   */
  public boolean isStorageObject() {
    return bucketName != null && objectName != null;
  }

  /**
   * Returns true if this StorageResourceId represents a GCS Bucket; if true, then {@code
   * getObjectName} will return null.
   */
  public boolean isBucket() {
    return bucketName != null && objectName == null;
  }

  /**
   * Returns true if this StorageResourceId represents the GCS root (gs://); if true, then
   * both {@code getBucketName} and {@code getObjectName} will be null.
   */
  public boolean isRoot() {
    return bucketName == null && objectName == null;
  }

  /**
   * Indicates if this StorageResourceId corresponds to a 'directory'; similar to
   * {@link FileInfo#isDirectory} except deals entirely with pathnames instead of also checking
   * for exists() to be true on a corresponding GoogleCloudStorageItemInfo.
   */
  public boolean isDirectory() {
    return isRoot() || isBucket() || objectHasDirectoryPath(objectName);
  }

  /**
   * Gets the bucket name component of this resource identifier.
   */
  public String getBucketName() {
    return bucketName;
  }

  /**
   * Gets the object name component of this resource identifier.
   */
  public String getObjectName() {
    return objectName;
  }

  /**
   * Returns a string of the form gs://<bucketName>/<objectName>.
   */
  @Override
  public String toString() {
    return readableString;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof StorageResourceId) {
      StorageResourceId other = (StorageResourceId) obj;
      return Objects.equals(bucketName, other.bucketName)
          && Objects.equals(objectName, other.objectName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return readableString.hashCode();
  }

  /**
   * Helper for standardizing the way various human-readable messages in logs/exceptions which refer
   * to a bucket/object pair.
   */
  public static String createReadableString(String bucketName, String objectName) {
    if (bucketName == null && objectName == null) {
      // TODO(user): Unify this method with other methods which convert bucketName/objectName
      // to a URI; maybe use the single slash for compatibility.
      return "gs://";
    } else if (bucketName != null && objectName == null) {
      return String.format("gs://%s", bucketName);
    } else if (bucketName != null && objectName != null) {
      return String.format("gs://%s/%s", bucketName, objectName);
    }
    throw new IllegalArgumentException(
        String.format("Invalid bucketName/objectName pair: gs://%s/%s", bucketName, objectName));
  }

  /**
   * Indicates whether the given object name looks like a directory path.
   *
   * @param objectName Name of the object to inspect.
   * @return Whether the given object name looks like a directory path.
   */
  static boolean objectHasDirectoryPath(String objectName) {
    return !Strings.isNullOrEmpty(objectName)
        && objectName.endsWith(GoogleCloudStorage.PATH_DELIMITER);
  }

  /**
   * Converts the given object name to look like a directory path.
   * If the object name already looks like a directory path then
   * this call is a no-op.
   * <p>
   * If the object name is null or empty, it is returned as-is.
   *
   * @param objectName Name of the object to inspect.
   * @return Directory path for the given path.
   */
  static String convertToDirectoryPath(String objectName) {
    if (!Strings.isNullOrEmpty(objectName)) {
      if (!objectHasDirectoryPath(objectName)) {
        objectName += GoogleCloudStorage.PATH_DELIMITER;
      }
    }
    return objectName;
  }
}
