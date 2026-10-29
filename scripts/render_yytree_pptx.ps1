$ErrorActionPreference = 'Stop'
$application = New-Object -ComObject PowerPoint.Application
if ($application.Presentations.Count -ne 0) {
    [Runtime.InteropServices.Marshal]::FinalReleaseComObject($application) | Out-Null
    throw 'PowerPoint already has open presentations; close them before running this test.'
}
try {
    foreach ($i in 1..10) {
        $path = [IO.Path]::GetFullPath("temp/yytree-pdf-vector/$i.pptx")
        $presentation = $application.Presentations.Open($path, -1, 0, 0)
        try {
            $presentation.Slides.Item(1).Export([IO.Path]::GetFullPath("temp/yytree-pdf-vector/$i-pptx.png"), 'PNG', 1600, 1200)
        } finally { $presentation.Close() }
    }
} finally {
    $application.Quit()
    [Runtime.InteropServices.Marshal]::FinalReleaseComObject($application) | Out-Null
}
