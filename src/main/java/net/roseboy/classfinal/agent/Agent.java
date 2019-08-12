package net.roseboy.classfinal.agent;

import net.roseboy.classfinal.Main;
import net.roseboy.classfinal.util.IOUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 监听类加载
 * <p>
 * 配置 -javaagent:this.jar='-data classes1.dat,classes2.dat -pwd 123123,000000'
 * 启动jar  java -javaagent:this.jar='-data aa.jar -pwd 0000000' -jar aa.jar
 *
 * java -javaagent:/Users/roseboy/code-space/agent/target/agent-1.0.jar='-data /Users/roseboy/work-yiyon/易用框架/yiyon-server-liuyuan/yiyon-package-liuyuan/target/yiyon-package-liuyuan-1.0.0-encrypted.jar -pwd 000000' -jar yiyon-package-liuyuan-1.0.0-encrypted.jar
 *
 * @author roseboy
 * @date 2019-08-02
 */
public class Agent {
    public static void premain(String args, Instrumentation inst) throws Exception {
        Main.printDog();

        Options options = new Options();
        options.addOption("data", true, "加密后的文件(多个用,分割)");
        options.addOption("pwd", true, "密码(多个用,分割)");

        String file = null;
        String pwd = null;
        if (args != null) {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args.split(" "));
            file = cmd.getOptionValue("data");
            pwd = cmd.getOptionValue("pwd");
        }

        if (file == null || file.length() == 0 || pwd == null || pwd.length() == 0) {
            return;
        }

        String[] files = file.split(",");
        String[] pwds = pwd.split(",");

        if (files.length != pwds.length) {
            throw new RuntimeException("加密文件和密码个数不一致");
        }
        for (int i = 0; i < files.length; i++) {
            //解压出classes.dat
            if (files[i].endsWith(".jar") || files[i].endsWith(".war")) {
                ZipFile zipFile = null;
                try {
                    File zip = new File(files[i]);
                    if (!zip.exists()) {
                        continue;
                    }
                    zipFile = new ZipFile(zip);
                    ZipEntry zipEntry = zipFile.getEntry(Main.FILE_NAME);
                    if (zipEntry == null) {
                        continue;
                    }
                    InputStream is = zipFile.getInputStream(zipEntry);
                    File classesDat = new File(files[i].substring(0, files[i].length() - 4) + "." + Main.FILE_NAME);
                    IOUtils.writeFile(classesDat, IOUtils.toByteArray(is));
                    files[i] = classesDat.getAbsolutePath();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.close(zipFile);
                }
            }
        }

        System.out.println(Arrays.toString(files));
        AgentTransformer tran = new AgentTransformer();
        tran.setFiles(files);
        tran.setPwds(pwds);
        inst.addTransformer(tran);
    }

}