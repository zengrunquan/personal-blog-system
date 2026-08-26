package com.blog.listener;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在线用户监听器
 * 统计当前在线用户数量
 *
 * @author blog-system
 */
@WebListener
public class OnlineUserListener implements HttpSessionListener {

    /** 在线用户数（使用原子类保证线程安全） */
    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        // Session创建时，在线人数+1
        int count = onlineCount.incrementAndGet();
        // 将在线人数存入ServletContext，供所有用户查看
        updateOnlineCount(event, count);
        System.out.println("[DEBUG] " + java.time.LocalDateTime.now() + " - 用户上线，当前在线人数：" + count);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        // Session销毁时，在线人数-1
        int count = onlineCount.decrementAndGet();
        if (count < 0) {
            onlineCount.set(0);
            count = 0;
        }
        updateOnlineCount(event, count);
        System.out.println("[DEBUG] " + java.time.LocalDateTime.now() + " - 用户下线，当前在线人数：" + count);
    }

    /**
     * 更新ServletContext中的在线人数
     */
    private void updateOnlineCount(HttpSessionEvent event, int count) {
        ServletContext context = event.getSession().getServletContext();
        context.setAttribute("onlineCount", count);
    }

    /**
     * 获取当前在线人数（静态方法，供其他地方调用）
     *
     * @return 在线人数
     */
    public static int getOnlineCount() {
        return onlineCount.get();
    }
}
