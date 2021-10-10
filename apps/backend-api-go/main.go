package main

func main() {

	conf := CreateConfig()
	
	app := &App{Config: conf}

	app.Init()
	app.Run()
}
