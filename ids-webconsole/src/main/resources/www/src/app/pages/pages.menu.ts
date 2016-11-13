export const PAGES_MENU = [
  {
    path: 'pages',
    children: [
      {
        path: 'dashboard',
        data: {
          menu: {
            title: 'Dashboard',
            icon: 'ion-android-home',
            selected: false,
            expanded: false,
            order: 0
          }
        }
      },
      {
        path: 'apps',
        data: {
          menu: {
            title: 'App Containers',
            icon: 'ion-cube',
            selected: false,
            expanded: false,
            order: 0
          }
        },
        children: [
          {
            path: 'installed-apps',
            data: {
              menu: {
                title: 'Installed Apps',
              }
            }
           }
        ]
      },
      {
        path: 'identities',
        data: {
          menu: {
            title: 'Identities',
            icon: 'ion-person-stalker',
            selected: false,
            expanded: false,
            order: 200,
          }
        },
        children: [
          {
            path: 'identities-my',
            data: {
              menu: {
                title: 'My Identity',
              }
            }
          },
          {
            path: 'identities-trusted',
            data: {
              menu: {
                title: 'Trusted Identities',
              }
            }
          }          
        ]
      },
      {
        path: 'pipes',
        data: {
          menu: {
            title: 'Data Pipes',
            icon: 'ion-network',
            selected: false,
            expanded: false,
            order: 0
          }
        }
      },
      {
        path: 'policies',
        data: {
          menu: {
            title: 'Policies',
            icon: 'ion-paper-outline',
            selected: false,
            expanded: false,
            order: 0
          }
        }
      },
      {
        path: 'settings',
        data: {
          menu: {
            title: 'Settings',
            icon: 'ion-android-settings',
            selected: false,
            expanded: false,
            order: 400,
            }
          }
        },
      {
        path: '',
        data: {
          menu: {
            title: 'Industrial Data Space',
            url: 'http://www.industrialdataspace.org',
            icon: 'ion-android-exit',
            order: 800,
            target: '_blank'
          }
        }
      }
    ]
  }
];
