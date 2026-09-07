package com.blog.media.maintenance;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 历史媒体回填命令；默认 dry-run，只有显式 --apply 才允许写媒体元数据和引用。 */
public final class MediaBackfillCommand {

    private static final Logger LOGGER = LogManager.getLogger(MediaBackfillCommand.class);

    private MediaBackfillCommand() {
    }

    public static void main(String[] args) {
        boolean apply = parseApply(args);
        try {
            MediaBackfillService.BackfillReport report = new MediaBackfillService().run(apply);
            System.out.println(new Gson().toJson(report.asMap()));
        } catch (Exception error) {
            LOGGER.error(
                    "[MediaBackfillCommand#main] 历史媒体回填执行失败，apply={}",
                    apply,
                    error
            );
            throw new IllegalStateException("历史媒体回填执行失败", error);
        }
    }

    static boolean parseApply(String[] args) {
        if (args == null || args.length == 0) return false;
        if (args.length == 1 && "--dry-run".equals(args[0])) return false;
        if (args.length == 1 && "--apply".equals(args[0])) return true;
        throw new IllegalArgumentException("参数只能是 --dry-run 或 --apply（默认 --dry-run）");
    }
}
