BEGIN TRANSACTION;
CREATE TABLE "artDb" (
	`_id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	`title`	TEXT,
	`latitude`	TEXT,
	`longitude`	TEXT
);
INSERT INTO `artDb` VALUES (1,'Inject1','41.800','12.434');
CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US');
INSERT INTO `android_metadata` VALUES ('en_US');
COMMIT;
