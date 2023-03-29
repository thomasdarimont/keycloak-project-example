use std::sync::mpsc::{self, TryRecvError};
use std::thread;
use std::time::Duration;

type Delay = Duration;
type Cancel = Box<dyn Fn() + Send>;

// Runs a given closure as a repeating job until the cancel callback is invoked.
// The jobs are run with a delay returned by the closure execution.
pub fn use_repeating_job<F>(job: F) -> Cancel
where
    F: Fn() -> Delay,
    F: Send + 'static,
{
    let (shutdown_tx, shutdown_rx) = mpsc::channel();

    thread::spawn(move || loop {
        let delay = job();
        thread::sleep(delay);

        if let Ok(_) | Err(TryRecvError::Disconnected) = shutdown_rx.try_recv() {
            break;
        }
    });

    Box::new(move || {
        println!("Stopping...");
        let _ = shutdown_tx.send("stop");
    })
}
