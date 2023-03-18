# DynmapProcessor

## Getting Started
Download it from [GitHub Releases](https://github.com/jaoafa/DynmapProcessor/releases), or build it yourself: `./gradlew package`  
Then, `java -jar ./DynmapProcessor-1.0.0.jar --help` to view help.
<details>
    <summary>Help output</summary>  

```
Usage: main [OPTIONS]

  Welcome to Dynmap Processor.

  For more detailed information, please refer to
  https://github.com/jaoafa/DynmapProcessor#readme

Options:
  -i, --input PATH    The directory of tile images.
  -o, --output PATH   The directory to output generated images and metadata.
  -c, --cache         Whether to allow the use of cached basemap. (Skip
                      basemap generation from scratch)
  -z, --zoom INT      Specify the zoom level from 0 to 4 (4 by default)
  -g, --grid          Whether to enable chunk grid.
  -e, --edit          Whether to enable image editing.
  -m, --markers PATH  The file path to the JSON file that configures markers.
  -t, --trim TEXT     Trim to the specified area. Format: x1,y1,x2,y2
  -h, --height INT    Height of the map image. Using this with the width
                      option might cause distortion.
  -w, --width INT     Width of the map image. Using this with the height
                      option might cause distortion.
  -r, --resize FLOAT  Scale up (or down) the output image to the specified
                      scale rate. (0<x<1 to scale down, 1<x to scale up)
  --help              Show this message and exit
```
</details>

## Options
| Type    | Name                                                | Flags            |  
|---------|-----------------------------------------------------|------------------|  
| `PATH`  | [Input](#input--i-or---input)                       | `-i` `--input`   |  
| `PATH`  | [Output](#output--o-or---output)                    | `-o` `--output`  |  
| `BOOL`  | [Cache](#cache--c-or---cache)                       | `-c` `--cache`   |  
| `INT`   | [Zoom level](#zoom-level--z-or---zoom-4-by-default) | `-z` `--zoom`    |  
| `BOOL`  | [Grid](#grid--g-or---grid-false-by-default)         | `-g` `--grid`    |  
| `BOOL`  | [Edit](#edit--e-or---edit-false-by-default)         | `-e` `--edit`    |
| `PATH`  | [Markers](#markers--m-or---markers)                 | `-m` `--markers` |
| `TEXT`  | [Trim](#trim--t-or---trim)                          | `-t` `--trim`    |
| `INT`   | [Height](#height-and-width--h-w-or---height--width) | `-h` `--height`  |
| `INT`   | [Width](#height-and-width--h-w-or---height--width)  | `-w` `--width`   |
| `FLOAT` | [Resize](#resize--r-or---resize-1-by-default)       | `-r` `--resize`  |

### Input: `-i` or `--input`
> Example: `-i ./input_images`  

The directory of tile images.  
The directory structure should be the following:
```
input_images
    zoom-0
        1_-12.png
        ...
    zoom-1
        ...
    zoom-2
        ...
    zoom-3
        ...
    zoom-4
        ...
```

### Output: `-o` or `--output`
> Example: `-o ./output_images`

The directory to output generated images and metadata.
```
output_images
    basemap.png
    map-2023-03-76-19-08-35.png
    map-2023-03-76-19-09-13.png
    metadata.json
```
`basemap.png` is the original unedited map image  
`map-{year}-{month}-{date}-{hour}-{minute}-{second}.png` is the edited image as you specified in [markers.json](#marker-file). How to edit image is [here](#edit--e-or---edit-false-by-default).  
`metadata.json` is the metadata file that stores resolution, central coordinate, and zoom level.

### Cache: `-c` or `--cache`
> Example: `-c`

Whether to allow the use of cached basemap.  
If you use this option and basemap.png already exists, Dynmap Processor will skip the new basemap generation.

### Zoom level: `-z` or `--zoom` (4 by default)
> Example: `-z 3`

Specify the zoom level from 0 to 4.  
**2~0 are super dangerous.**

### Grid: `-g` or `--grid` (`false` by default)
> Example: `-g`

Whether to enable chunk grid.

### Edit: `-e` or `--edit` (`false` by default)
> Example: `-e`

Whether to enable image editing.  
The edited image is output like [this](#output--o-or---output).
**Note that if you forget to add this option, Dynmap Processor will NOT edit the image.**  
Edit options are all the following.  
[Markers](#markers--m-or---markers), [Trim](#trim--t-or---trim), [Height](#height-and-width--h-w-or---height--width), [Width](#height-and-width--h-w-or---height--width), and [Resize](#resize--r-or---resize-1-by-default).  
Note that the final image will be processed in the following order:  
1. draw markers `-m`  
2. trim to a specified area `-t`  
3. scale width and height `-h,-w`  
4. resize the image by the provided rate `-r`  

### Markers: `-m` or `--markers`
> Example: `-m ./markers.json`

The file path to the JSON file configures markers.  
See [Marker file](#marker-file)

### Trim: `-t` or `--trim`
> Example: `-t 1000,1000,-1000,-1000`

Trim to the specified area.  
Format: `x1,y1,x2,y2`

### Height and width: `-h`,`-w` or `--height`,`--width`
> Example: `-w 1000` / `-h 500`

Height or width of the map image.  
Using both option at the same time might cause distortion.

### Resize: `-r` or `--resize` (`1` by default)
> Example: `-r 0.8`

Scale up (or down) the output image to the specified scale rate.  
`0<x<1` to scale down, `1<x` to scale up.

## Marker file
JSON Schema: `` //todo url
<details>
    <summary>Sample file</summary>

```json
{
  "markers": [
    {
      "type": "Area",
      "name": "yuuaCity",
      "coordinates": [
        {
          "x": 1070,
          "y": -1873
        },
        {
          "x": 1070,
          "y": -1764
        },
        {
          "x": 704,
          "y": -1764
        },
        {
          "x": 704,
          "y": -1430
        },
        {
          "x": 1362,
          "y": -1873
        },
        {
          "x": 1362,
          "y": -1430
        }
      ],
      "color": {
        "r": 231,
        "g": 214,
        "b": 243
      },
      "overlay": {
        "r": 231,
        "g": 214,
        "b": 243,
        "a": 100
      }
    },
    {
      "type": "Line",
      "name": "Sample Line",
      "coordinates": [
        {
          "x": 511,
          "y": -511
        },
        {
          "x": -511,
          "y": 511
        }
      ],
      "color": {
        "r": 255,
        "g": 161,
        "b": 38
      },
      "overlay": {
        "r": 255,
        "g": 161,
        "b": 38,
        "a": 100
      }
    },
    {
      "type": "Circle",
      "name": "Sample Circle",
      "radius": 100,
      "coordinates": [
        {
          "x": 0,
          "y": 31
        }
      ],
      "color": {
        "r": 187,
        "g": 200,
        "b": 230
      },
      "overlay": {
        "r": 187,
        "g": 200,
        "b": 230,
        "a": 100
      }
    }
  ]
}
```
</details>


## Example
`java -jar ./DynmapProcessor-1.0.0.jar -i ./images -o ./output -z 4 -e -m ./markers.json`
