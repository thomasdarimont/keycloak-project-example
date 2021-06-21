import { join } from "path";
import express from "express";
import serveStatic from "serve-static";
import serveIndex from "serve-index";

const servePath = join(__dirname, "../html");

const PORT = 3000;

const app = express();
express.static.mime.define({ "text/html": ["ftl"] });

app.use(serveStatic(servePath + "/"));
app.use(serveIndex(servePath, { view: "details" }));

app.listen(PORT);
console.log(
  `Serving Email Templates from ${servePath} on http://localhost:${PORT}`
);
