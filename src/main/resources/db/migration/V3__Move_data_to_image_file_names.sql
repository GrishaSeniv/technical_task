UPDATE `fish`
SET `image_file_names` = JSON_ARRAY(`image_file_name`)
WHERE `image_file_name` IS NOT NULL;

ALTER TABLE `fish`
    DROP COLUMN `image_file_name`;