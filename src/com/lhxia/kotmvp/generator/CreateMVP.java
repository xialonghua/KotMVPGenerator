package com.lhxia.kotmvp.generator;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiClassUtil;
import com.intellij.util.JavaPsiConstructorUtil;
import com.intellij.util.ThrowableRunnable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.io.VelocityWriter;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

public class CreateMVP extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                CreateMVPDialog createMVPDialog = new CreateMVPDialog(){
                    @Override
                    protected void onOK(String contractName, String packageName, int typeIndex) {
                        super.onOK(contractName, packageName, typeIndex);
                        try {
                            WriteAction.run(() -> gen(event, contractName, packageName, typeIndex));
                        } catch (IOException throwable) {
                            throwable.printStackTrace();
                        }

                    }
                };
                createMVPDialog.setBounds(0, 0, 400, 400);
                createMVPDialog.setLocationByPlatform(true);
                createMVPDialog.setVisible(true);

            }
        });
    }

    private void gen(AnActionEvent actionEvent, String contractName, String packageName, int typeIndex) throws IOException {

        if (contractName == null || contractName.length() == 0){
            return;
        }
        IdeView ideView = actionEvent.getRequiredData(LangDataKeys.IDE_VIEW);
        PsiDirectory directory = ideView.getOrChooseDirectory();
        if (directory == null){
            return;
        }
        //当前工程
        Project project = actionEvent.getProject();

        Velocity.init();
        VelocityContext context = new VelocityContext();
        context.put("ModuleName", contractName);
        context.put("ModuleNameLowerCase", contractName.toLowerCase());
        if (packageName == null || packageName.length() == 0){
            context.put("PACKAGE_NAME", getFilePackageName(directory.getVirtualFile()));
        }else {
            context.put("PACKAGE_NAME", packageName);
        }
        context.put("APP_PACKAGE_NAME", getPackageName(project));

        //鼠标右键所选择的路径

        if (directory.findFile(contractName + "Contract.kt") == null && directory.findFile(contractName + "PresenterImpl.kt") == null) {
            parse(directory, context, "contract.vm", contractName, "Contract.kt");
            parse(directory, context, "presenter.vm", contractName, "PresenterImpl.kt");


            if (typeIndex == 1){//ACTIVITY
                if (directory.findFile(contractName + "Activity.kt") == null) {
                    parse(directory, context, "view_activity.vm", contractName, "Activity.kt");
                    createLayoutXML(project, directory, "activity_" + contractName.toLowerCase() + ".xml");
                }

            }else if (typeIndex == 2){//FRAGMENT
                if (directory.findFile(contractName + "Fragment.kt") == null) {
                    parse(directory, context, "view_fragment.vm", contractName, "Fragment.kt");
                    createLayoutXML(project, directory, "fragment_" + contractName.toLowerCase() + ".xml");
                }
            }

        }
    }

    private static void createLayoutXML(Project project, PsiDirectory directory, String filename) throws IOException {
        if(!directory.isDirectory()) {
            // 非目录的取所在文件夹路径
            directory = directory.getParent();
        }
        VirtualFile virtualFile = directory.getVirtualFile();
        while (true){
            String childName = virtualFile.getName();
            virtualFile = virtualFile.getParent();
            directory = directory.getParent();
            if (childName.equals("java") && virtualFile.getName().equals("main")){
                VirtualFile parent = virtualFile.getParent();
                if (parent.getName().equals("src")){
                    break;
                }
            }
        }
        PsiDirectory layoutDir = directory.findSubdirectory("res").findSubdirectory("layout");
        PsiFile layoutFile = layoutDir.findFile(filename);
        if (layoutFile == null){
            parse(layoutDir, new VelocityContext(), "layout.vm", filename, "");
        }

    }

    private static void parse(PsiDirectory directory,
                             VelocityContext context,
                             String tmpName,
                             String moduleName, String fileType) throws IOException {
        InputStream in = CreateMVP.class.getResourceAsStream(tmpName);
        StringWriter writer = null;
        try {

            String s = IOUtils.toString(in, "utf-8");
            writer = new StringWriter();
            Velocity.evaluate(context, writer, "", s);
            writer.flush();
            PsiFile psiFile = directory.createFile(moduleName + fileType);
            psiFile.getVirtualFile().setBinaryContent(writer.toString().getBytes());
        } finally {
            if (writer != null){
                writer.close();
            }
            if (in != null){
                in.close();
            }
        }

    }

    private static String getFilePackageName(VirtualFile dir) {
        if(!dir.isDirectory()) {
            // 非目录的取所在文件夹路径
            dir = dir.getParent();
        }
        String path = dir.getPath().replace("/", ".");
        String preText = "src.main.java";
        int preIndex = path.indexOf(preText) + preText.length() + 1;
        path = path.substring(preIndex);
        return path;
    }

    /**
     * AndroidManifest.xml 获取 app 包名
     * @return
     */
    private static String getPackageName(Project project) {
        String package_name = "";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(project.getBasePath() + "/app/src/main/AndroidManifest.xml");
            NodeList nodeList = doc.getElementsByTagName("manifest");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element element = (Element) node;
                package_name = element.getAttribute("package");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return package_name;
    }

}
