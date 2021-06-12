import glob from "glob";
import fs from "fs-extra";
import mjml2html from "mjml";

const inputPath = "./mjml";
const filePath = "./common";
const outputPath = "./html";
const extension = ".ftl";

const compileFile = (filename: string) => {
  const content = fs.readFileSync(filename, "utf8");

  const parseResult = mjml2html(content, { filePath });

  if (parseResult.errors.length) {
    console.log(`MJML Errors in file: ${filename}`);
    parseResult.errors.forEach((e) => console.log(e));
  }

  const outputFilename = filename
    .replace(inputPath, outputPath)
    .replace(".mjml", extension);

  fs.ensureFileSync(outputFilename);
  fs.writeFileSync(outputFilename, parseResult.html);
};

fs.ensureDirSync(outputPath);

glob("./mjml/**/*.mjml", (_, files: string[]) => files.forEach(compileFile));
