package tw.ouyang.javawebshell;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LinuxShellController {

    private String currentDirectory = "/home/ubuntu/Microservices-Practice";

    @RequestMapping(value = "command/**", method = RequestMethod.PATCH)
    public String command(HttpServletRequest request, @RequestBody String requestBody) throws Exception {
        String url = request.getRequestURL().toString();
        String[] inputString = url.split("/command/")[1].split("%20");
        StringBuilder output = new StringBuilder();
        if ("cd".equals(inputString[0]) && inputString.length == 2) {
            if (Paths.get(inputString[1]).isAbsolute()) {
                currentDirectory = inputString[1];
            } else {
                currentDirectory = Paths.get(currentDirectory, inputString[1]).toRealPath().toString();
            }
            output.append("url: " + url + "\n");
            output.append("currentDirectory: " + currentDirectory);
            return output.toString();
        } else if ("vim".equals(inputString[0])) {
            String[] bodyLines = requestBody.split("\n");
            Process process = Runtime.getRuntime().exec("rm -rf %s " + inputString[1], null, new File(currentDirectory));
            process.waitFor();
            for (String bodyLine : bodyLines) {
                if (bodyLine.isBlank()) {
                    process = Runtime.getRuntime().exec(String.format("cat.>> %s", inputString[1]), null, new File(currentDirectory));
                } else {
                    process = Runtime.getRuntime().exec(String.format("cat %s>> %s", bodyLine.trim(), inputString[1]), null, new File(currentDirectory));
                }
                process.waitFor();
            }
            process = Runtime.getRuntime().exec("cat %s " + inputString[1], null, new File(currentDirectory));
            return waitProcessAndGetOutput(process, output);
        } else if ("curl".equals(inputString[0])) {
            StringBuilder command = new StringBuilder();
            for (String data : inputString) {
                command.append(" " + data);
            }
            if (!request.getParameterMap().isEmpty()) {
                command.append("?");
                request.getParameterMap().forEach((key, value) -> {
                    command.append(String.format("%s=%s&", key, value[0]));
                });
            }
            output.append("url: " + url + "\n");
            output.append("command: " + command.toString() + "\n\n");
            Process process = Runtime.getRuntime().exec(command.toString(), null, new File(currentDirectory));
            return waitProcessAndGetOutput(process, output);
        } else {
            StringBuilder command = new StringBuilder();
            for (String data : inputString) {
                command.append(" " + data);
            }
            output.append("url: " + url + "\n");
            output.append("command: " + command.toString() + "\n\n");
            Process process = Runtime.getRuntime().exec(command.toString(), null, new File(currentDirectory));
            return waitProcessAndGetOutput(process, output);
        }
    }

    private String waitProcessAndGetOutput(Process process, StringBuilder output) throws InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        reader.lines().forEach(line -> {
            output.append(line + "\n");
        });
        process.waitFor();
        return output.toString();
    }

}
