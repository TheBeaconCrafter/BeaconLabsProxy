package org.bcnlab.beaconlabsproxy;

import com.sun.management.OperatingSystemMXBean;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class ServerMetricsCommand extends Command implements TabExecutor {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.servermetrics";

    public ServerMetricsCommand(BeaconLabsProxy plugin) {
        super("servermetrics", PERMISSION, "sm");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double cpuLoad = osBean.getSystemCpuLoad() * 100;
        long totalMemory = osBean.getTotalPhysicalMemorySize();
        long freeMemory = osBean.getFreePhysicalMemorySize();
        double memUsage = totalMemory > 0 ? ((double)(totalMemory - freeMemory) / totalMemory) * 100 : 0;

        // Disk usage
        File[] roots = File.listRoots();
        StringBuilder diskInfo = new StringBuilder();
        for (File root : roots) {
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            double usedPercent = totalSpace > 0 ? ((double)(totalSpace - freeSpace) / totalSpace) * 100 : 0;
            diskInfo.append(String.format("%s: %.2f%% used; ", root.getPath(), usedPercent));
        }

        int processCount = getProcessCount();
        int netIfCount = getNetworkInterfaceCount();

        sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GOLD + "Server Metrics:"));
        sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.YELLOW + String.format("CPU Load: %.2f%%", cpuLoad)));
        sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.YELLOW + String.format("Memory Usage: %.2f%% (%s/%s)", memUsage,
                humanReadableBytes(totalMemory - freeMemory), humanReadableBytes(totalMemory))));
        sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.YELLOW + "Disk Usage: " + diskInfo.toString()));
        sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.YELLOW + "Process Count: " + processCount));
        sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.YELLOW + "Network Interfaces: " + netIfCount));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    private int getProcessCount() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                Process process = new ProcessBuilder("cmd", "/c", "tasklist").start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int lines = 0;
                while (reader.readLine() != null) lines++;
                reader.close();
                return Math.max(0, lines - 3);
            } catch (IOException e) {
                return -1;
            }
        } else {
            try {
                Process process = new ProcessBuilder("/bin/sh", "-c", "ps -e | wc -l").start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                reader.close();
                return Integer.parseInt(line.trim());
            } catch (Exception e) {
                return -1;
            }
        }
    }

    private int getNetworkInterfaceCount() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            int count = 0;
            while (nets.hasMoreElements()) {
                nets.nextElement();
                count++;
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }

    private static String humanReadableBytes(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = ("KMGTPE").charAt(exp - 1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
