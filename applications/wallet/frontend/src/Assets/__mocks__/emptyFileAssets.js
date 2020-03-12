const emptyFileAssets = {
  count: 7,
  next:
    'http://localhost:8000/api/v1/projects/00000000-0000-0000-0000-000000000000/assets/?from=2&size=2',
  previous: null,
  results: [
    {
      id: 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C',
      metadata: {
        system: {
          jobId: '106d4be1-5d95-11ea-baea-364023b135f5',
          dataSourceId: '10509c20-5d95-11ea-baea-364023b135f5',
          timeCreated: '2020-03-03T21:22:24.922700Z',
          state: 'Analyzed',
          projectId: '89382033-bcbb-4c8f-81a9-3d0883298a81',
          taskId: '106e0f32-5d95-11ea-baea-364023b135f5',
          timeModified: '2020-03-03T21:22:47.575195Z',
        },
        source: {
          path: 'gs://zorroa-dev-data/image/TIFF_1MB.tiff',
          extension: 'tiff',
          filename: 'TIFF_1MB.tiff',
          mimetype: 'image/tiff',
          filesize: 1131930,
          checksum: 1867533868,
          url:
            'https://localhost:3000/api/v1/projects/89382033-bcbb-4c8f-81a9-3d0883298a81/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/files/source/TIFF_1MB.tiff',
        },
        metrics: {
          pipeline: [
            {
              processor:
                'zmlp_core.core.processors.PreCacheSourceFileProcessor',
              module: 'standard',
              checksum: 1621235190,
              executionTime: 0.11,
              executionDate: '2020-03-03T21:22:27.694149',
            },
            {
              processor: 'zmlp_core.image.importers.ImageImporter',
              module: 'standard',
              checksum: 1426657387,
              executionTime: 0.26,
              executionDate: '2020-03-03T21:22:28.215069',
            },
            {
              processor: 'zmlp_core.office.importers.OfficeImporter',
              module: 'standard',
              checksum: 2001473853,
            },
            {
              processor: 'zmlp_core.video.VideoImporter',
              module: 'standard',
              checksum: 3310423168,
            },
            {
              processor: 'zmlp_core.core.processors.AssertAttributesProcessor',
              module: 'standard',
              checksum: 1841569083,
              executionTime: 0,
              executionDate: '2020-03-03T21:22:29.654215',
            },
          ],
        },
        media: {
          width: 650,
          height: 434,
          aspect: 1.5,
          orientation: 'landscape',
          type: 'image',
          length: 1,
        },
        clip: {
          type: 'page',
          start: 1,
          stop: 1,
          length: 1,
          pile: 'pUn6wBxUN7x9JxOxLkvruOyNdYA',
          sourceAssetId: 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C',
        },
        files: [],
        analysis: {
          zvi: {
            tinyProxy: [
              '#f3dfc3',
              '#f4efd8',
              '#c18f46',
              '#ebdfbd',
              '#ccd3c0',
              '#e7d4bb',
              '#beae8e',
              '#cabf9e',
              '#d2c09c',
            ],
          },
        },
        similarity: {
          simhash:
            'NMGPEKEACJCHEDDBCHFAPBAHNPPPKMPOPCMODAPNFPCPOPBNEPCPPBHPPCBPDBPJHBGPPPNKOMBHBPNPAHLFCPPPFPGBKPPFDPPHDFAADNKGADBGPFFFGKABGPDLFPPLPPNPPPAAJBPPPLDPKAMCKANPAGCBPBPCEPEAKMHEHPBLKIIFJPOPBPPMGONPPGPFDPCJNPPMPIPPPPHPPCBJPHPGJDNBMGBAPAEBCFDPLPPAJLKANAPPACCPPEAMPADJMCAIPIKNNPDAPMPPCAAOPBADBDJPHGEKFGIDBPHDPPAMPPHLBGPPPHPEPPNAPPPAHOPCLAKPPPFAAHCPFCFPAPBPILPADJAFMJDPCPMCKBBBFBGPLPJMEDDMHPGHMMJPPABAJCAGCFJFNJPBPEPCPCINFEHPHCCPLPGPOIPPMPNIPNDDELAPILCEAPPAIPPDPPBGEFPJLFPJPIECMAEPABALFGLPPLFDPGDABPLMJDPDGBHFPPBPPCPFDPPDFJPAPCDGGGPPGPCPPFIKCHPPPBPLJBPPJIPHLPPKGKDPBCCLNGMAPFHACDINFPPEPBHDLCPGPFPPKPDAJPBEPPPPGPIADNAPABCKHIKPBDLPBPHPLAPFPPDBBEPEHPPPPFBJFANJPMPCFHMDEPDPMNIBDPPAHDBPPNECHLJAIDJFPOLPPBHLABEHPGPPKBAKIPJPBOPPBPHCBIPPAOPIPBAJHBMPPCMADHMPMFKPDEDOPBBAGNNACAADPHDCEKPDPGKHPBFPGJLPPINPPPBPPEANCPPAPBKBFGPAIPEFPFDJGPHAPCPIJHLCBEPPMPPCFPNEPIAPAAEPDHFBMHCNACPFFFDPBLMPPHEMAAIJODGCGCHIGDMAPHDNFEFMHPLPPBAKFBPPAPFPCGJNEDBOBPCLNIIIAEFGAGPGIEAFNPCCCBPKIDIPAPPPPOBPFPADEPPDCECMBFJPAKOEHPBKPGEOBFLNPFAIPPCKPCPHPAIPLPPLEJFFIMICPAPMEPKDNBDOFPJEPCAJEJKEMLHCGENAPEPACPPNPCCLDINPHCDBPHADKIFEPCIPKJIOFOPDEPGEDEPPGPDMBJKPBEHBEPLPFNPJGPJPBPHJHGHPAAEKDIEEAEAGJFPCGLPELPILCAPCFPELIICCPCLPJHCADFIPIGCFLIKPGPJGDCALPBDJPPEGCPLJFGEPBHDPPBPPPIKGDEEDENMBLLIGNECBPEFPBMJKCGPDPJOCNDNLANPKAPPPGPLBBBAJIGEADBEJFFNEGPNKPPPBPGEAELPKLDEGAANIFCLBPPPPNDJGDAFEEACPNEPKKPPKGAHLPDPGPBKPFBDIDDAMPPAIHPHHAPAEGOPCPJEABPPPBAKPLABPCPHNPAEMDPPPPDAJANPPDMPEGFIDEEAAJLGPHPHPCLDPDPFIDCAPPPHIPPPPEDHJDBBPHFKBAOPPPFGEPJPLPCDPBAJCPPDPEFGBFECPIAOGHAPEPAMPNPCPBAPODHNDCIPAGPPPFCPPDMFCLCEHPHAEACACIPCDPPPOMLAKMPPBGDPLFDBMFAPCMIKPHFPPCPPDFPPBPPKPPPPPFECBFIPKKEIPPPPBCKPLEPDEPKCHEPCEJDEKABHBOPPAAPIKABKHBPHPGPDBPHAPJKEDBPBBPLCPFDEEKHEPDFNPGJPAPPOPCCCJPPKPAEDIFPADNADAPDPPEMFPMPPLMPIPIADLPPPFINEPBBBAFBKIFPDPPPEBNPFBEMPFCCEGHEJIMACOCCPHBJPPNKPIPACDPBJPFKPAPIDNPCIGGIPHJGPCKELBLAAPLBBHHHPPCBHPLFAPPJPEEPLHPGPFAOOPPFPJHILPDECMJACKHHAPANLPGALJBNAFODHMJCPPFPJICDPOJBGPKFFPGIJPOHPJAIEPBBKCBIIMIPFPPPAGDDHHHIGGKHFJPPPPEKBGHDCHDPAGEPPIPDPIKALNPHJBGIPPNFINIDPPPEPPCBPPLMFCLGGPDGGLKINGBGKEPHPBPBFHAOBGAHCDGNEBDDCGPDCP',
        },
      },
      url:
        'https://localhost:3000/api/v1/projects/89382033-bcbb-4c8f-81a9-3d0883298a81/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/',
    },
  ],
}

export default emptyFileAssets
