package mooni.board.common.outboxmessagerelay;

import lombok.Getter;

import java.util.List;
import java.util.stream.LongStream;

@Getter
public class AssignedShard {

    // 애플리케이션에 할당된 샤드
    private List<Long> shards;

    public static AssignedShard of(String appId, List<String> appIds, long shardCount) {

        AssignedShard assignedShard = new AssignedShard();

        assignedShard.shards = assign(appId, appIds, shardCount);

        return assignedShard;
    }

    private static List<Long> assign(String appId, List<String> appIds, long shardCount) {

        int appIndex = findAppIndex(appId, appIds);

        // 할당할 샤드가 없으면 빈 배열 반환
        if (appIndex == -1) {
            return List.of();
        }

        long start = appIndex * shardCount / appIds.size();
        long end = (appIndex + 1) * shardCount / appIds.size() - 1;

        return LongStream.rangeClosed(start, end).boxed().toList();
    }

    private static int findAppIndex(String appId, List<String> appIds) {
        for (int i = 0; i < appIds.size(); i++) {
            if (appIds.get(i).equals(appId)) {
                return i;
            }
        }
        return -1;
    }
}
