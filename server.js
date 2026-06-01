const http = require("http"), fs = require("fs"), path = require("path");
const mime = { html: "text/html; charset=utf-8", json: "application/json", js: "text/javascript", css: "text/css", png: "image/png", svg: "image/svg+xml" };
http.createServer((q, s) => {
    let fp = q.url === "/" ? "sheep.html" : q.url.replace(/^\//, "");
    fp = path.join(__dirname, fp);
    fs.readFile(fp, (e, d) => {
        if (e) { s.writeHead(404); s.end("Not found"); return; }
        s.writeHead(200, { "Content-Type": mime[path.extname(fp).slice(1)] || "text/plain", "Cache-Control": "no-cache" });
        s.end(d);
    });
}).listen(8080, () => console.log("Server on :8080"));
